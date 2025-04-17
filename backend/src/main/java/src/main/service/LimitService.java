package src.main.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import src.main.dto.category.CategoryResponse;
import src.main.dto.limit.LimitRequest;
import src.main.dto.limit.LimitResponse;
import src.main.model.Category;
import src.main.model.Limit;
import src.main.model.LimitPeriod;
import src.main.model.User;
import src.main.repository.CategoryRepository;
import src.main.repository.LimitRepository;
import src.main.repository.TransactionRepository;
import src.main.repository.UserRepository;
import src.main.service.ProfileService;
import src.main.util.DateUtil;
import src.main.exception.ConflictException;
import src.main.exception.DuplicateResourceException;
import src.main.exception.InvalidDataException;
import src.main.exception.OperationNotAllowedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LimitService {

    private final LimitRepository limitRepository;
    private final ProfileService profileService;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Cacheable(value = "limits", key = "#root.target.getCurrentUserEmail()")
    public List<LimitResponse> getLimits() {
        User currentUser = profileService.getCurrentUser();
        log.debug("Получение лимитов для пользователя {}", currentUser.getEmail());
        List<Limit> limits = limitRepository.findByUser(currentUser);
        log.info("Найдено {} лимитов для пользователя {}", limits.size(), currentUser.getEmail());
        return limits.stream()
                .map(this::calculateUsageAndMap)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "limits", key = "#root.target.getCurrentUserEmail()")
    public LimitResponse createLimit(LimitRequest request) {
        User currentUser = profileService.getCurrentUser();
        log.debug("Создание лимита для пользователя {}: {}", currentUser.getEmail(), request);

        LimitPeriod periodEnum;
        try {
            periodEnum = LimitPeriod.valueOf(request.getPeriod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Недопустимый период лимита: " + request.getPeriod());
        }

        if (limitRepository.existsByCategory_IdAndPeriodAndUser(request.getCategoryId(), periodEnum, currentUser)) {
            throw new DuplicateResourceException("Лимит для данной категории и периода уже существует");
        }

        Limit limit = Limit.builder()
                .amount(request.getAmount())
                .build();

        limit.setUser(currentUser);
        limit.setPeriod(periodEnum);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Категория с ID " + request.getCategoryId() + " не найдена"));
        limit.setCategory(category);

        Limit savedLimit = limitRepository.save(limit);
        log.info("Лимит (ID: {}) успешно создан для пользователя {}", savedLimit.getId(), currentUser.getEmail());
        return calculateUsageAndMap(savedLimit);
    }

    @CacheEvict(value = "limits", key = "#root.target.getCurrentUserEmail()")
    public LimitResponse updateLimit(Integer id, LimitRequest request) {
        User currentUser = profileService.getCurrentUser();
        log.debug("Обновление лимита с ID: {} для пользователя {}", id, currentUser.getEmail());

        Limit limit = limitRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new EntityNotFoundException("Лимит с ID " + id + " не найден для пользователя"));

        LimitPeriod requestedPeriodEnum;
        try {
            requestedPeriodEnum = LimitPeriod.valueOf(request.getPeriod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Недопустимый период лимита: " + request.getPeriod());
        }

        if ((!limit.getCategory().getId().equals(request.getCategoryId()) || !limit.getPeriod().equals(requestedPeriodEnum)) &&
            limitRepository.existsByCategory_IdAndPeriodAndUser(request.getCategoryId(), requestedPeriodEnum, currentUser)) {
             throw new DuplicateResourceException("Лимит для данной категории и периода уже существует");
        }

        limit.setAmount(request.getAmount());
        limit.setPeriod(requestedPeriodEnum);

        if (!limit.getCategory().getId().equals(request.getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Категория с ID " + request.getCategoryId() + " не найдена"));
            limit.setCategory(category);
        }

        Limit updatedLimit = limitRepository.save(limit);
        log.info("Лимит (ID: {}) успешно обновлен для пользователя {}", updatedLimit.getId(), currentUser.getEmail());
        return calculateUsageAndMap(updatedLimit);
    }

    @CacheEvict(value = "limits", key = "#root.target.getCurrentUserEmail()")
    public void deleteLimit(Integer id) {
        User currentUser = profileService.getCurrentUser();
        log.debug("Удаление лимита с ID: {} для пользователя {}", id, currentUser.getEmail());
        Limit limit = limitRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new EntityNotFoundException("Лимит с ID " + id + " не найден для пользователя"));
        limitRepository.delete(limit);
        log.info("Лимит с ID {} успешно удален для пользователя {}", id, currentUser.getEmail());
    }

    public LimitResponse getLimitById(Integer id) {
        User currentUser = profileService.getCurrentUser();
        log.debug("Получение лимита по ID: {} для пользователя {}", id, currentUser.getEmail());
        Limit limit = limitRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new EntityNotFoundException("Лимит с ID " + id + " не найден для пользователя"));
        return calculateUsageAndMap(limit);
    }

    private LimitResponse calculateUsageAndMap(Limit limit) {
        if (limit == null) {
            return null;
        }
        LocalDateTime startDate = DateUtil.getStartDateFromPeriod(limit.getPeriod().name());
        BigDecimal currentUsage = transactionRepository.sumAmountByUserAndCategoryAndDateAfter(
                limit.getUser(), limit.getCategory(), startDate);

        currentUsage = (currentUsage == null) ? BigDecimal.ZERO : currentUsage;

        LimitResponse response = LimitResponse.builder()
                .id(limit.getId())
                .categoryId(limit.getCategory() != null ? limit.getCategory().getId() : null)
                .categoryName(limit.getCategory() != null ? limit.getCategory().getName() : null)
                .amount(limit.getAmount())
                .period(limit.getPeriod().name())
                .userId(limit.getUser() != null ? limit.getUser().getId().toString() : null)
                .currentUsage(currentUsage.abs())
                .build();

        return response;
    }
} 