package com.openjava.cas.web.rest;

import com.openjava.cas.AbstractCassandraTest;
import com.openjava.cas.Casoauth2App;
import com.openjava.cas.domain.User;
import com.openjava.cas.repository.UserRepository;
import com.openjava.cas.security.AuthoritiesConstants;
import com.openjava.cas.service.MailService;
import com.openjava.cas.service.UserService;
import com.openjava.cas.service.dto.UserDTO;
import com.openjava.cas.service.mapper.UserMapper;
import com.openjava.cas.service.util.RandomUtil;
import com.openjava.cas.web.rest.errors.ExceptionTranslator;
import com.openjava.cas.web.rest.vm.ManagedUserVM;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Casoauth2App.class)
public class UserResourceIntTest extends AbstractCassandraTest {

    private static final String DEFAULT_ID = "id1";

    private static final String DEFAULT_LOGIN = "johndoe";
    private static final String UPDATED_LOGIN = "jhipster";

    private static final String DEFAULT_PASSWORD = "passjohndoe";
    private static final String UPDATED_PASSWORD = "passjhipster";

    private static final String DEFAULT_EMAIL = "johndoe@localhost";
    private static final String UPDATED_EMAIL = "jhipster@localhost";

    private static final String DEFAULT_FIRSTNAME = "john";
    private static final String UPDATED_FIRSTNAME = "jhipsterFirstName";

    private static final String DEFAULT_LASTNAME = "doe";
    private static final String UPDATED_LASTNAME = "jhipsterLastName";

    private static final String DEFAULT_LANGKEY = "en";
    private static final String UPDATED_LANGKEY = "fr";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    private MockMvc restUserMockMvc;

    private User user;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        UserResource userResource = new UserResource(userRepository, mailService, userService);
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(userResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .build();
    }

    /**
     * Create a User.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which has a required relationship to the User entity.
     */
    public static User createEntity() {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setLogin(DEFAULT_LOGIN);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setEmail(DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setLangKey(DEFAULT_LANGKEY);
        return user;
    }

    @Before
    public void initTest() {
        userRepository.deleteAll();
        user = createEntity();
    }

    @Test
    public void createUser() throws Exception {
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        // Create the User
        Set<String> authorities = new HashSet<>();
        authorities.add("ROLE_USER");
        ManagedUserVM managedUserVM = new ManagedUserVM(
            null,
            DEFAULT_LOGIN,
            DEFAULT_PASSWORD,
            DEFAULT_FIRSTNAME,
            DEFAULT_LASTNAME,
            DEFAULT_EMAIL,
            true,
            DEFAULT_LANGKEY,
            authorities);

        restUserMockMvc.perform(post("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isCreated());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate + 1);
        User testUser = userList.get(userList.size() - 1);
        assertThat(testUser.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(testUser.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(testUser.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(testUser.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testUser.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
    }

    @Test
    public void createUserWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        Set<String> authorities = new HashSet<>();
        authorities.add("ROLE_USER");
        ManagedUserVM managedUserVM = new ManagedUserVM(
            UUID.randomUUID().toString(),
            DEFAULT_LOGIN,
            DEFAULT_PASSWORD,
            DEFAULT_FIRSTNAME,
            DEFAULT_LASTNAME,
            DEFAULT_EMAIL,
            true,
            DEFAULT_LANGKEY,
            authorities);

        // An entity with an existing ID cannot be created, so this API call must fail
        restUserMockMvc.perform(post("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isBadRequest());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void createUserWithExistingLogin() throws Exception {
        // Initialize the database
        userRepository.save(user);
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        Set<String> authorities = new HashSet<>();
        authorities.add("ROLE_USER");
        ManagedUserVM managedUserVM = new ManagedUserVM(
            null,
            DEFAULT_LOGIN, // this login should already be used
            DEFAULT_PASSWORD,
            DEFAULT_FIRSTNAME,
            DEFAULT_LASTNAME,
            "anothermail@localhost",
            true,
            DEFAULT_LANGKEY,
            authorities);

        // Create the User
        restUserMockMvc.perform(post("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isBadRequest());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void createUserWithExistingEmail() throws Exception {
        // Initialize the database
        userRepository.save(user);
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        Set<String> authorities = new HashSet<>();
        authorities.add("ROLE_USER");
        ManagedUserVM managedUserVM = new ManagedUserVM(
            null,
            "anotherlogin",
            DEFAULT_PASSWORD,
            DEFAULT_FIRSTNAME,
            DEFAULT_LASTNAME,
            DEFAULT_EMAIL, // this email should already be used
            true,
            DEFAULT_LANGKEY,
            authorities);

        // Create the User
        restUserMockMvc.perform(post("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isBadRequest());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void getAllUsers() throws Exception {
        // Initialize the database
        userRepository.save(user);

        // Get all the users
        restUserMockMvc.perform(get("/api/users")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN)))
            .andExpect(jsonPath("$.[*].firstName").value(hasItem(DEFAULT_FIRSTNAME)))
            .andExpect(jsonPath("$.[*].lastName").value(hasItem(DEFAULT_LASTNAME)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].langKey").value(hasItem(DEFAULT_LANGKEY)));
    }

    @Test
    public void getUser() throws Exception {
        // Initialize the database
        userRepository.save(user);

        // Get the user
        restUserMockMvc.perform(get("/api/users/{login}", user.getLogin()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.login").value(user.getLogin()))
            .andExpect(jsonPath("$.firstName").value(DEFAULT_FIRSTNAME))
            .andExpect(jsonPath("$.lastName").value(DEFAULT_LASTNAME))
            .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
            .andExpect(jsonPath("$.langKey").value(DEFAULT_LANGKEY));
    }

    @Test
    public void getNonExistingUser() throws Exception {
        restUserMockMvc.perform(get("/api/users/unknown"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateUser() throws Exception {
        // Initialize the database
        userRepository.save(user);
        int databaseSizeBeforeUpdate = userRepository.findAll().size();

        // Update the user
        User updatedUser = userRepository.findOne(user.getId());

        Set<String> authorities = new HashSet<>();
        authorities.add("ROLE_USER");
        ManagedUserVM managedUserVM = new ManagedUserVM(
            updatedUser.getId(),
            updatedUser.getLogin(),
            UPDATED_PASSWORD,
            UPDATED_FIRSTNAME,
            UPDATED_LASTNAME,
            UPDATED_EMAIL,
            updatedUser.getActivated(),
            UPDATED_LANGKEY,
            authorities);

        restUserMockMvc.perform(put("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isOk());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeUpdate);
        User testUser = userList.get(userList.size() - 1);
        assertThat(testUser.getFirstName()).isEqualTo(UPDATED_FIRSTNAME);
        assertThat(testUser.getLastName()).isEqualTo(UPDATED_LASTNAME);
        assertThat(testUser.getEmail()).isEqualTo(UPDATED_EMAIL);
        assertThat(testUser.getLangKey()).isEqualTo(UPDATED_LANGKEY);
    }

    @Test
    public void updateUserLogin() throws Exception {
        // Initialize the database
        userRepository.save(user);
        int databaseSizeBeforeUpdate = userRepository.findAll().size();

        // Update the user
        User updatedUser = userRepository.findOne(user.getId());

        Set<String> authorities = new HashSet<>();
        authorities.add("ROLE_USER");
        ManagedUserVM managedUserVM = new ManagedUserVM(
            updatedUser.getId(),
            UPDATED_LOGIN,
            UPDATED_PASSWORD,
            UPDATED_FIRSTNAME,
            UPDATED_LASTNAME,
            UPDATED_EMAIL,
            updatedUser.getActivated(),
            UPDATED_LANGKEY,
            authorities);

        restUserMockMvc.perform(put("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isOk());

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeUpdate);
        User testUser = userList.get(userList.size() - 1);
        assertThat(testUser.getLogin()).isEqualTo(UPDATED_LOGIN);
        assertThat(testUser.getFirstName()).isEqualTo(UPDATED_FIRSTNAME);
        assertThat(testUser.getLastName()).isEqualTo(UPDATED_LASTNAME);
        assertThat(testUser.getEmail()).isEqualTo(UPDATED_EMAIL);
        assertThat(testUser.getLangKey()).isEqualTo(UPDATED_LANGKEY);
    }

    @Test
    public void updateUserExistingEmail() throws Exception {
        // Initialize the database with 2 users
        userRepository.save(user);

        User anotherUser = new User();
        anotherUser.setId(UUID.randomUUID().toString());
        anotherUser.setLogin("jhipster");
        anotherUser.setPassword(RandomStringUtils.random(60));
        anotherUser.setActivated(true);
        anotherUser.setEmail("jhipster@localhost");
        anotherUser.setFirstName("java");
        anotherUser.setLastName("hipster");
        anotherUser.setLangKey("en");
        userRepository.save(anotherUser);

        // Update the user
        User updatedUser = userRepository.findOne(user.getId());

        Set<String> authorities = new HashSet<>();
        authorities.add("ROLE_USER");
        ManagedUserVM managedUserVM = new ManagedUserVM(
            updatedUser.getId(),
            updatedUser.getLogin(),
            updatedUser.getPassword(),
            updatedUser.getFirstName(),
            updatedUser.getLastName(),
            "jhipster@localhost",  // this email should already be used by anotherUser
            updatedUser.getActivated(),
            updatedUser.getLangKey(),
            authorities);

        restUserMockMvc.perform(put("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void updateUserExistingLogin() throws Exception {
        // Initialize the database
        userRepository.save(user);

        User anotherUser = new User();
        anotherUser.setId(UUID.randomUUID().toString());
        anotherUser.setLogin("jhipster");
        anotherUser.setPassword(RandomStringUtils.random(60));
        anotherUser.setActivated(true);
        anotherUser.setEmail("jhipster@localhost");
        anotherUser.setFirstName("java");
        anotherUser.setLastName("hipster");
        anotherUser.setLangKey("en");
        userRepository.save(anotherUser);

        // Update the user
        User updatedUser = userRepository.findOne(user.getId());

        Set<String> authorities = new HashSet<>();
        authorities.add("ROLE_USER");
        ManagedUserVM managedUserVM = new ManagedUserVM(
            updatedUser.getId(),
            "jhipster", // this login should already be used by anotherUser
            updatedUser.getPassword(),
            updatedUser.getFirstName(),
            updatedUser.getLastName(),
            updatedUser.getEmail(),
            updatedUser.getActivated(),
            updatedUser.getLangKey(),
            authorities);

        restUserMockMvc.perform(put("/api/users")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteUser() throws Exception {
        // Initialize the database
        userRepository.save(user);
        int databaseSizeBeforeDelete = userRepository.findAll().size();

        // Delete the user
        restUserMockMvc.perform(delete("/api/users/{login}", user.getLogin())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    public void testUserEquals() throws Exception {
        TestUtil.equalsVerifier(User.class);
        User user1 = new User();
        user1.setId("id1");
        User user2 = new User();
        user2.setId(user1.getId());
        assertThat(user1).isEqualTo(user2);
        user2.setId("id2");
        assertThat(user1).isNotEqualTo(user2);
        user1.setId(null);
        assertThat(user1).isNotEqualTo(user2);
    }

    @Test
    public void testUserFromId() {
        assertThat(userMapper.userFromId(DEFAULT_ID).getId()).isEqualTo(DEFAULT_ID);
        assertThat(userMapper.userFromId(null)).isNull();
    }

    @Test
    public void testUserDTOtoUser() {
        UserDTO userDTO = new UserDTO(
            DEFAULT_ID,
            DEFAULT_LOGIN,
            DEFAULT_FIRSTNAME,
            DEFAULT_LASTNAME,
            DEFAULT_EMAIL,
            true,
            DEFAULT_LANGKEY,
            Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet()));
        User user = userMapper.userDTOToUser(userDTO);
        assertThat(user.getId()).isEqualTo(DEFAULT_ID);
        assertThat(user.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(user.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(user.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(user.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(user.getActivated()).isEqualTo(true);
        assertThat(user.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
        assertThat(user.getAuthorities()).containsExactly(AuthoritiesConstants.USER);
    }

    @Test
    public void testUserToUserDTO() {
        user.setId(DEFAULT_ID);
        user.setAuthorities(Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet()));

        UserDTO userDTO = userMapper.userToUserDTO(user);

        assertThat(userDTO.getId()).isEqualTo(DEFAULT_ID);
        assertThat(userDTO.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(userDTO.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(userDTO.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(userDTO.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(userDTO.isActivated()).isEqualTo(true);
        assertThat(userDTO.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
        assertThat(userDTO.getAuthorities()).containsExactly(AuthoritiesConstants.USER);
        assertThat(userDTO.toString()).isNotNull();
    }

}
