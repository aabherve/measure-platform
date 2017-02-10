package org.measure.platform.web.rest;

import org.measure.platform.MeasurePlatformApp;
import org.measure.platform.core.api.entitys.MeasureViewService;
import org.measure.platform.core.entity.MeasureView;
import org.measure.platform.core.impl.repository.MeasureViewRepository;
import org.measure.platform.restapi.app.entitys.MeasureViewResource;
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
 * Test class for the MeasureViewResource REST controller.
 *
 * @see MeasureViewResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MeasurePlatformApp.class)
public class MeasureViewResourceIntTest {

    private static final String DEFAULT_VIEW_DATA = "AAAAA";
    private static final String UPDATED_VIEW_DATA = "BBBBB";

    @Inject
    private MeasureViewRepository measureViewRepository;

    @Inject
    private MeasureViewService measureViewService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restMeasureViewMockMvc;

    private MeasureView measureView;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        MeasureViewResource measureViewResource = new MeasureViewResource();
        ReflectionTestUtils.setField(measureViewResource, "measureViewService", measureViewService);
        this.restMeasureViewMockMvc = MockMvcBuilders.standaloneSetup(measureViewResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MeasureView createEntity(EntityManager em) {
        MeasureView measureView = new MeasureView()
                .viewData(DEFAULT_VIEW_DATA);
        return measureView;
    }

    @Before
    public void initTest() {
        measureView = createEntity(em);
    }

    @Test
    @Transactional
    public void createMeasureView() throws Exception {
        int databaseSizeBeforeCreate = measureViewRepository.findAll().size();

        // Create the MeasureView

        restMeasureViewMockMvc.perform(post("/api/measure-views")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(measureView)))
                .andExpect(status().isCreated());

        // Validate the MeasureView in the database
        List<MeasureView> measureViews = measureViewRepository.findAll();
        assertThat(measureViews).hasSize(databaseSizeBeforeCreate + 1);
        MeasureView testMeasureView = measureViews.get(measureViews.size() - 1);
        assertThat(testMeasureView.getViewData()).isEqualTo(DEFAULT_VIEW_DATA);
    }

    @Test
    @Transactional
    public void getAllMeasureViews() throws Exception {
        // Initialize the database
        measureViewRepository.saveAndFlush(measureView);

        // Get all the measureViews
        restMeasureViewMockMvc.perform(get("/api/measure-views?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(measureView.getId().intValue())))
                .andExpect(jsonPath("$.[*].viewData").value(hasItem(DEFAULT_VIEW_DATA.toString())));
    }

    @Test
    @Transactional
    public void getMeasureView() throws Exception {
        // Initialize the database
        measureViewRepository.saveAndFlush(measureView);

        // Get the measureView
        restMeasureViewMockMvc.perform(get("/api/measure-views/{id}", measureView.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(measureView.getId().intValue()))
            .andExpect(jsonPath("$.viewData").value(DEFAULT_VIEW_DATA.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingMeasureView() throws Exception {
        // Get the measureView
        restMeasureViewMockMvc.perform(get("/api/measure-views/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateMeasureView() throws Exception {
        // Initialize the database
        measureViewService.save(measureView);

        int databaseSizeBeforeUpdate = measureViewRepository.findAll().size();

        // Update the measureView
        MeasureView updatedMeasureView = measureViewRepository.findOne(measureView.getId());
        updatedMeasureView
                .viewData(UPDATED_VIEW_DATA);

        restMeasureViewMockMvc.perform(put("/api/measure-views")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedMeasureView)))
                .andExpect(status().isOk());

        // Validate the MeasureView in the database
        List<MeasureView> measureViews = measureViewRepository.findAll();
        assertThat(measureViews).hasSize(databaseSizeBeforeUpdate);
        MeasureView testMeasureView = measureViews.get(measureViews.size() - 1);
        assertThat(testMeasureView.getViewData()).isEqualTo(UPDATED_VIEW_DATA);
    }

    @Test
    @Transactional
    public void deleteMeasureView() throws Exception {
        // Initialize the database
        measureViewService.save(measureView);

        int databaseSizeBeforeDelete = measureViewRepository.findAll().size();

        // Get the measureView
        restMeasureViewMockMvc.perform(delete("/api/measure-views/{id}", measureView.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<MeasureView> measureViews = measureViewRepository.findAll();
        assertThat(measureViews).hasSize(databaseSizeBeforeDelete - 1);
    }
}
