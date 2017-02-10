package org.measure.platform.web.rest;

import org.measure.platform.MeasurePlatformApp;
import org.measure.platform.core.api.entitys.MeasurePropertyService;
import org.measure.platform.core.entity.MeasureProperty;
import org.measure.platform.core.impl.repository.MeasurePropertyRepository;
import org.measure.platform.restapi.app.entitys.MeasurePropertyResource;
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
 * Test class for the MeasurePropertyResource REST controller.
 *
 * @see MeasurePropertyResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MeasurePlatformApp.class)
public class MeasurePropertyResourceIntTest {

    private static final String DEFAULT_PROPERTY_NAME = "AAAAA";
    private static final String UPDATED_PROPERTY_NAME = "BBBBB";

    private static final String DEFAULT_PROPERTY_VALUE = "AAAAA";
    private static final String UPDATED_PROPERTY_VALUE = "BBBBB";

    @Inject
    private MeasurePropertyRepository measurePropertyRepository;

    @Inject
    private MeasurePropertyService measurePropertyService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restMeasurePropertyMockMvc;

    private MeasureProperty measureProperty;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        MeasurePropertyResource measurePropertyResource = new MeasurePropertyResource();
        ReflectionTestUtils.setField(measurePropertyResource, "measurePropertyService", measurePropertyService);
        this.restMeasurePropertyMockMvc = MockMvcBuilders.standaloneSetup(measurePropertyResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MeasureProperty createEntity(EntityManager em) {
        MeasureProperty measureProperty = new MeasureProperty()
                .propertyName(DEFAULT_PROPERTY_NAME)
                .propertyValue(DEFAULT_PROPERTY_VALUE);
        return measureProperty;
    }

    @Before
    public void initTest() {
        measureProperty = createEntity(em);
    }

    @Test
    @Transactional
    public void createMeasureProperty() throws Exception {
        int databaseSizeBeforeCreate = measurePropertyRepository.findAll().size();

        // Create the MeasureProperty

        restMeasurePropertyMockMvc.perform(post("/api/measure-properties")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(measureProperty)))
                .andExpect(status().isCreated());

        // Validate the MeasureProperty in the database
        List<MeasureProperty> measureProperties = measurePropertyRepository.findAll();
        assertThat(measureProperties).hasSize(databaseSizeBeforeCreate + 1);
        MeasureProperty testMeasureProperty = measureProperties.get(measureProperties.size() - 1);
        assertThat(testMeasureProperty.getPropertyName()).isEqualTo(DEFAULT_PROPERTY_NAME);
        assertThat(testMeasureProperty.getPropertyValue()).isEqualTo(DEFAULT_PROPERTY_VALUE);
    }

    @Test
    @Transactional
    public void checkPropertyNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = measurePropertyRepository.findAll().size();
        // set the field null
        measureProperty.setPropertyName(null);

        // Create the MeasureProperty, which fails.

        restMeasurePropertyMockMvc.perform(post("/api/measure-properties")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(measureProperty)))
                .andExpect(status().isBadRequest());

        List<MeasureProperty> measureProperties = measurePropertyRepository.findAll();
        assertThat(measureProperties).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllMeasureProperties() throws Exception {
        // Initialize the database
        measurePropertyRepository.saveAndFlush(measureProperty);

        // Get all the measureProperties
        restMeasurePropertyMockMvc.perform(get("/api/measure-properties?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(measureProperty.getId().intValue())))
                .andExpect(jsonPath("$.[*].propertyName").value(hasItem(DEFAULT_PROPERTY_NAME.toString())))
                .andExpect(jsonPath("$.[*].propertyValue").value(hasItem(DEFAULT_PROPERTY_VALUE.toString())));
    }

    @Test
    @Transactional
    public void getMeasureProperty() throws Exception {
        // Initialize the database
        measurePropertyRepository.saveAndFlush(measureProperty);

        // Get the measureProperty
        restMeasurePropertyMockMvc.perform(get("/api/measure-properties/{id}", measureProperty.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(measureProperty.getId().intValue()))
            .andExpect(jsonPath("$.propertyName").value(DEFAULT_PROPERTY_NAME.toString()))
            .andExpect(jsonPath("$.propertyValue").value(DEFAULT_PROPERTY_VALUE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingMeasureProperty() throws Exception {
        // Get the measureProperty
        restMeasurePropertyMockMvc.perform(get("/api/measure-properties/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateMeasureProperty() throws Exception {
        // Initialize the database
        measurePropertyService.save(measureProperty);

        int databaseSizeBeforeUpdate = measurePropertyRepository.findAll().size();

        // Update the measureProperty
        MeasureProperty updatedMeasureProperty = measurePropertyRepository.findOne(measureProperty.getId());
        updatedMeasureProperty
                .propertyName(UPDATED_PROPERTY_NAME)
                .propertyValue(UPDATED_PROPERTY_VALUE);

        restMeasurePropertyMockMvc.perform(put("/api/measure-properties")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedMeasureProperty)))
                .andExpect(status().isOk());

        // Validate the MeasureProperty in the database
        List<MeasureProperty> measureProperties = measurePropertyRepository.findAll();
        assertThat(measureProperties).hasSize(databaseSizeBeforeUpdate);
        MeasureProperty testMeasureProperty = measureProperties.get(measureProperties.size() - 1);
        assertThat(testMeasureProperty.getPropertyName()).isEqualTo(UPDATED_PROPERTY_NAME);
        assertThat(testMeasureProperty.getPropertyValue()).isEqualTo(UPDATED_PROPERTY_VALUE);
    }

    @Test
    @Transactional
    public void deleteMeasureProperty() throws Exception {
        // Initialize the database
        measurePropertyService.save(measureProperty);

        int databaseSizeBeforeDelete = measurePropertyRepository.findAll().size();

        // Get the measureProperty
        restMeasurePropertyMockMvc.perform(delete("/api/measure-properties/{id}", measureProperty.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<MeasureProperty> measureProperties = measurePropertyRepository.findAll();
        assertThat(measureProperties).hasSize(databaseSizeBeforeDelete - 1);
    }
}
