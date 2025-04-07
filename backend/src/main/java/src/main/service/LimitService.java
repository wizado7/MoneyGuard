package src.main.service;

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
import src.main.util.DateUtil;
import src.main.exception.EntityNotFoundException;
import src.main.exception.InvalidDataException;
import src.main.exception.ConflictException;
import src.main.exception.OperationNotAllowedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LimitService {

    private final LimitRepository limitRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "limits", key = "#root.target.getCurrentUserEmail()")
    public List<LimitResponse> getLimits() {
        User currentUser = getCurrentUser();
        
        List<Limit> limits = limitRepository.findByUser(currentUser);
        
        return limits.stream()
                .map(this::mapToLimitResponse)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "limits", key = "#root.target.getCurrentUserEmail()")
    public LimitResponse createLimit(LimitRequest request) {
        log.debug("Создание нового лимита");
        User currentUser = getCurrentUser();
        
        // Проверяем категорию
        Category category = categoryRepository.findById(request.getCategory_id())
                .orElseThrow(() -> {
                    log.warn("Категория с ID {} не найдена", request.getCategory_id());
                    return new EntityNotFoundException("Категория", request.getCategory_id());
                });
        
        // Проверяем, что категория принадлежит текущему пользователю
        if (!category.getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка использования чужой категории: {} пользователем: {}", request.getCategory_id(), currentUser.getEmail());
            throw OperationNotAllowedException.notOwner("категория");
        }
        
        // Проверяем, существует ли уже лимит для этой категории
        limitRepository.findByUserAndCategory(currentUser, category)
                .ifPresent(limit -> {
                    log.warn("Попытка создания дублирующего лимита для категории: {}", category.getName());
                    throw ConflictException.entityExists("Лимит", "категория", category.getName());
                });
        
        // Проверяем период
        LimitPeriod period;
        try {
            period = LimitPeriod.valueOf(request.getPeriod());
        } catch (IllegalArgumentException e) {
            log.warn("Некорректное значение периода: {}", request.getPeriod());
            throw new InvalidDataException("Некорректное значение периода")
                    .addError("period", "Период должен быть одним из: DAILY, WEEKLY, MONTHLY, YEARLY");
        }
        
        Limit limit = Limit.builder()
                .user(currentUser)
                .category(category)
                .amount(request.getAmount())
                .period(period)
                .build();
        
        limitRepository.save(limit);
        log.info("Лимит успешно создан для категории: {} пользователем: {}", category.getName(), currentUser.getEmail());
        
        // Рассчитываем текущие расходы
        BigDecimal currentSpending = transactionRepository.sumByUserAndCategoryAndPeriod(
                currentUser, category, DateUtil.getStartOfPeriod(period), LocalDate.now());
        
        return LimitResponse.builder()
                .id(limit.getId())
                .category(mapToCategoryResponse(category))
                .amount(limit.getAmount())
                .period(limit.getPeriod().name())
                .current_spending(currentSpending != null ? currentSpending : BigDecimal.ZERO)
                .build();
    }

    @CacheEvict(value = "limits", key = "#root.target.getCurrentUserEmail()")
    public LimitResponse updateLimit(Long id, LimitRequest request) {
        log.debug("Обновление лимита с ID: {}", id);
        User currentUser = getCurrentUser();
        
        Limit limit = limitRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Лимит с ID {} не найден", id);
                    return new EntityNotFoundException("Лимит", id);
                });
        
        // Проверяем, что лимит принадлежит текущему пользователю
        if (!limit.getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка обновления чужого лимита: {} пользователем: {}", id, currentUser.getEmail());
            throw OperationNotAllowedException.notOwner("лимит");
        }
        
        // Проверяем категорию
        Category category = categoryRepository.findById(request.getCategory_id())
                .orElseThrow(() -> {
                    log.warn("Категория с ID {} не найдена", request.getCategory_id());
                    return new EntityNotFoundException("Категория", request.getCategory_id());
                });
        
        // Проверяем, что категория принадлежит текущему пользователю
        if (!category.getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка использования чужой категории: {} пользователем: {}", request.getCategory_id(), currentUser.getEmail());
            throw OperationNotAllowedException.notOwner("категория");
        }
        
        // Проверяем, что для этой категории нет другого лимита (кроме текущего)
        limitRepository.findByUserAndCategory(currentUser, category)
                .ifPresent(existingLimit -> {
                    if (!existingLimit.getId().equals(id)) {
                        log.warn("Попытка создания дублирующего лимита для категории: {}", category.getName());
                        throw ConflictException.entityExists("Лимит", "категория", category.getName());
                    }
                });
        
        // Проверяем период
        LimitPeriod period;
        try {
            period = LimitPeriod.valueOf(request.getPeriod());
        } catch (IllegalArgumentException e) {
            log.warn("Некорректное значение периода: {}", request.getPeriod());
            throw new InvalidDataException("Некорректное значение периода")
                    .addError("period", "Период должен быть одним из: DAILY, WEEKLY, MONTHLY, YEARLY");
        }
        
        limit.setCategory(category);
        limit.setAmount(request.getAmount());
        limit.setPeriod(period);
        
        limitRepository.save(limit);
        log.info("Лимит успешно обновлен для категории: {} пользователем: {}", category.getName(), currentUser.getEmail());
        
        // Рассчитываем текущие расходы
        BigDecimal currentSpending = transactionRepository.sumByUserAndCategoryAndPeriod(
                currentUser, category, DateUtil.getStartOfPeriod(period), LocalDate.now());
        
        return LimitResponse.builder()
                .id(limit.getId())
                .category(mapToCategoryResponse(category))
                .amount(limit.getAmount())
                .period(limit.getPeriod().name())
                .current_spending(currentSpending != null ? currentSpending : BigDecimal.ZERO)
                .build();
    }

    @CacheEvict(value = "limits", key = "#root.target.getCurrentUserEmail()")
    public void deleteLimit(Long id) {
        log.debug("Удаление лимита с ID: {}", id);
        User currentUser = getCurrentUser();
        
        Limit limit = limitRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Лимит с ID {} не найден", id);
                    return new EntityNotFoundException("Лимит", id);
                });
        
        // Проверяем, что лимит принадлежит текущему пользователю
        if (!limit.getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка удаления чужого лимита: {} пользователем: {}", id, currentUser.getEmail());
            throw OperationNotAllowedException.notOwner("лимит");
        }
        
        limitRepository.delete(limit);
        log.info("Лимит успешно удален для категории: {} пользователем: {}", limit.getCategory().getName(), currentUser.getEmail());
    }

    private LimitResponse mapToLimitResponse(Limit limit) {
        // Получаем даты периода для расчета текущих трат
        LocalDate dateFrom;
        LocalDate dateTo = LocalDate.now();
        
        switch (limit.getPeriod()) {
            case DAILY:
                dateFrom = dateTo;
                break;
            case WEEKLY:
                dateFrom = DateUtil.getStartOfWeek(dateTo);
                break;
            case MONTHLY:
                dateFrom = DateUtil.getStartOfMonth(dateTo);
                break;
            case YEARLY:
                dateFrom = DateUtil.getStartOfYear(dateTo);
                break;
            default:
                dateFrom = dateTo;
        }
        
        // Получаем сумму трат по категории за период
        BigDecimal currentSpending = transactionRepository.sumByUserAndCategoryAndPeriod(
                limit.getUser(), limit.getCategory(), dateFrom, dateTo);
        
        // Если нет трат, устанавливаем 0
        if (currentSpending == null) {
            currentSpending = BigDecimal.ZERO;
        }
        
        return LimitResponse.builder()
                .id(limit.getId())
                .category(mapToCategoryResponse(limit.getCategory()))
                .amount(limit.getAmount())
                .period(limit.getPeriod().name())
                .current_spending(currentSpending)
                .build();
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .icon(category.getIcon())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    public String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
} 