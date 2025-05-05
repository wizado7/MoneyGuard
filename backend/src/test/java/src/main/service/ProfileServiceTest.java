package src.main.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import src.main.dto.profile.ProfileResponse;
import src.main.dto.profile.ProfileUpdateRequest;
import src.main.model.User;
import src.main.repository.UserRepository;
import src.main.exception.DuplicateResourceException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileService profileService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPassword("encodedPassword");

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@example.com");
    }

    @Test
    void getProfile_ShouldReturnCurrentUserProfile() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        ProfileResponse result = profileService.getProfile();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test User", result.getName());
    }

    @Test
    void updateProfile_ShouldUpdateProfileWithoutPassword() {
        // Arrange
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setEmail("new@example.com");
        request.setName("New Name");
        request.setPassword(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        ProfileResponse result = profileService.updateProfile(request);

        // Assert
        assertNotNull(result);
        assertEquals("new@example.com", result.getEmail());
        assertEquals("New Name", result.getName());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void updateProfile_ShouldUpdateProfileWithPassword() {
        // Arrange
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setEmail("test@example.com");
        request.setName("New Name");
        request.setPassword("newPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        ProfileResponse result = profileService.updateProfile(request);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("New Name", result.getName());
        verify(passwordEncoder, times(1)).encode("newPassword");
    }

    @Test
    void updateProfile_ShouldThrowWhenEmailExists() {
        // Arrange
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setEmail("existing@example.com");
        request.setName("Test User");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> profileService.updateProfile(request));
    }

    @Test
    void getCurrentUser_ShouldReturnCurrentUser() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        User result = profileService.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }
}