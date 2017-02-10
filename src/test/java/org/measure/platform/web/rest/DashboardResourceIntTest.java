package org.measure.platform.web.rest;

import org.measure.platform.MeasurePlatformApp;
import org.measure.platform.core.api.entitys.DashboardService;
import org.measure.platform.core.entity.Dashboard;
import org.measure.platform.core.impl.repository.DashboardRepository;
import org.measure.platform.restapi.app.entitys.DashboardResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the DashboardResource REST controller.
 *
 * @see DashboardResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MeasurePlatformApp.class)
public class DashboardResourceIntTest {

    private static final String DEFAULT_DASHBOARD_NAME = "AAAAA";
    private static final String UPDATED_DASHBOARD_NAME = "BBBBB";

    private static final String DEFAULT_DASHBOARD_DESCRIPTION = "AAAAA";
    private static final String UPDATED_DASHBOARD_DESCRIPTION = "BBBBB";

    @Inject
    private DashboardRepository dashboardRepository;

    @Inject
    private DashboardService dashboardService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restDashboardMockMvc;

    private Dashboard dashboard;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        DashboardResource dashboardResource = new DashboardResource();
        ReflectionTestUtils.setField(dashboardResource, "dashboardService", dashboardService);
        this.restDashboardMockMvc = MockMvcBuilders.standaloneSetup(dashboardResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Dashboard createEntity(EntityManager em) {
        Dashboard dashboard = new Dashboard()
                .dashboardName(DEFAULT_DASHBOARD_NAME)
                .dashboardDescription(DEFAULT_DASHBOARD_DESCRIPTION);
        return dashboard;
    }

    @Before
    public void initTest() {
        dashboard = createEntity(em);
    }

    @Test
    @Transactional
    public void createDashboard() throws Exception {
        int databaseSizeBeforeCreate = dashboardRepository.findAll().size();

        // Create the Dashboard

        restDashboardMockMvc.perform(post("/api/dashboards")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(dashboard)))
                .andExpect(status().isCreated());

        // Validate the Dashboard in the database
        List<Dashboard> dashboards = dashboardRepository.findAll();
        assertThat(dashboards).hasSize(databaseSizeBeforeCreate + 1);
        Dashboard testDashboard = dashboards.get(dashboards.size() - 1);
        assertThat(testDashboard.getDashboardName()).isEqualTo(DEFAULT_DASHBOARD_NAME);
        assertThat(testDashboard.getDashboardDescription()).isEqualTo(DEFAULT_DASHBOARD_DESCRIPTION);
    }

    @Test
    @Transactional
    public void checkDashboardNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = dashboardRepository.findAll().size();
        // set the field null
        dashboard.setDashboardName(null);

        // Create the Dashboard, which fails.

        restDashboardMockMvc.perform(post("/api/dashboards")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(dashboard)))
                .andExpect(status().isBadRequest());

        List<Dashboard> dashboards = dashboardRepository.findAll();
        assertThat(dashboards).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllDashboards() throws Exception {
        // Initialize the database
        dashboardRepository.saveAndFlush(dashboard);

        // Get all the dashboards
        restDashboardMockMvc.perform(get("/api/dashboards?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(dashboard.getId().intValue())))
                .andExpect(jsonPath("$.[*].dashboardName").value(hasItem(DEFAULT_DASHBOARD_NAME.toString())))
                .andExpect(jsonPath("$.[*].dashboardDescription").value(hasItem(DEFAULT_DASHBOARD_DESCRIPTION.toString())));
    }

    @Test
    @Transactional
    public void getDashboard() throws Exception {
        // Initialize the database
        dashboardRepository.saveAndFlush(dashboard);

        // Get the dashboard
        restDashboardMockMvc.perform(get("/api/dashboards/{id}", dashboard.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(dashboard.getId().intValue()))
            .andExpect(jsonPath("$.dashboardName").value(DEFAULT_DASHBOARD_NAME.toString()))
            .andExpect(jsonPath("$.dashboardDescription").value(DEFAULT_DASHBOARD_DESCRIPTION.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingDashboard() throws Exception {
        // Get the dashboard
        restDashboardMockMvc.perform(get("/api/dashboards/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateDashboard() throws Exception {
        // Initialize the database
        dashboardService.save(dashboard);

        int databaseSizeBeforeUpdate = dashboardRepository.findAll().size();

        // Update the dashboard
        Dashboard updatedDashboard = dashboardRepository.findOne(dashboard.getId());
        updatedDashboard
                .dashboardName(UPDATED_DASHBOARD_NAME)
                .dashboardDescription(UPDATED_DASHBOARD_DESCRIPTION);

        restDashboardMockMvc.perform(put("/api/dashboards")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedDashboard)))
                .andExpect(status().isOk());

        // Validate the Dashboard in the database
        List<Dashboard> dashboards = dashboardRepository.findAll();
        assertThat(dashboards).hasSize(databaseSizeBeforeUpdate);
        Dashboard testDashboard = dashboards.get(dashboards.size() - 1);
        assertThat(testDashboard.getDashboardName()).isEqualTo(UPDATED_DASHBOARD_NAME);
        assertThat(testDashboard.getDashboardDescription()).isEqualTo(UPDATED_DASHBOARD_DESCRIPTION);
    }

    @Test
    @Transactional
    public void deleteDashboard() throws Exception {
        // Initialize the database
        dashboardService.save(dashboard);

        int databaseSizeBeforeDelete = dashboardRepository.findAll().size();

        // Get the dashboard
        restDashboardMockMvc.perform(delete("/api/dashboards/{id}", dashboard.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Dashboard> dashboards = dashboardRepository.findAll();
        assertThat(dashboards).hasSize(databaseSizeBeforeDelete - 1);
    }
}
