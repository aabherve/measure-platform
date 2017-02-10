package org.measure.platform.web.rest;

import org.measure.platform.MeasurePlatformApp;
import org.measure.platform.core.api.entitys.PhaseService;
import org.measure.platform.core.entity.Phase;
import org.measure.platform.core.impl.repository.PhaseRepository;
import org.measure.platform.restapi.app.entitys.PhaseResource;
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
 * Test class for the PhaseResource REST controller.
 *
 * @see PhaseResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MeasurePlatformApp.class)
public class PhaseResourceIntTest {

    private static final String DEFAULT_PHASE_NAME = "AAAAA";
    private static final String UPDATED_PHASE_NAME = "BBBBB";

    private static final String DEFAULT_PHASE_DESCRIPTION = "AAAAA";
    private static final String UPDATED_PHASE_DESCRIPTION = "BBBBB";

    private static final String DEFAULT_PHASE_IMAGE = "AAAAA";
    private static final String UPDATED_PHASE_IMAGE = "BBBBB";

    @Inject
    private PhaseRepository phaseRepository;

    @Inject
    private PhaseService phaseService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restPhaseMockMvc;

    private Phase phase;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PhaseResource phaseResource = new PhaseResource();
        ReflectionTestUtils.setField(phaseResource, "phaseService", phaseService);
        this.restPhaseMockMvc = MockMvcBuilders.standaloneSetup(phaseResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Phase createEntity(EntityManager em) {
        Phase phase = new Phase()
                .phaseName(DEFAULT_PHASE_NAME)
                .phaseDescription(DEFAULT_PHASE_DESCRIPTION);
        return phase;
    }

    @Before
    public void initTest() {
        phase = createEntity(em);
    }

    @Test
    @Transactional
    public void createPhase() throws Exception {
        int databaseSizeBeforeCreate = phaseRepository.findAll().size();

        // Create the Phase

        restPhaseMockMvc.perform(post("/api/phases")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(phase)))
                .andExpect(status().isCreated());

        // Validate the Phase in the database
        List<Phase> phases = phaseRepository.findAll();
        assertThat(phases).hasSize(databaseSizeBeforeCreate + 1);
        Phase testPhase = phases.get(phases.size() - 1);
        assertThat(testPhase.getPhaseName()).isEqualTo(DEFAULT_PHASE_NAME);
        assertThat(testPhase.getPhaseDescription()).isEqualTo(DEFAULT_PHASE_DESCRIPTION);
    }

    @Test
    @Transactional
    public void checkPhaseNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = phaseRepository.findAll().size();
        // set the field null
        phase.setPhaseName(null);

        // Create the Phase, which fails.

        restPhaseMockMvc.perform(post("/api/phases")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(phase)))
                .andExpect(status().isBadRequest());

        List<Phase> phases = phaseRepository.findAll();
        assertThat(phases).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllPhases() throws Exception {
        // Initialize the database
        phaseRepository.saveAndFlush(phase);

        // Get all the phases
        restPhaseMockMvc.perform(get("/api/phases?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(phase.getId().intValue())))
                .andExpect(jsonPath("$.[*].phaseName").value(hasItem(DEFAULT_PHASE_NAME.toString())))
                .andExpect(jsonPath("$.[*].phaseDescription").value(hasItem(DEFAULT_PHASE_DESCRIPTION.toString())))
                .andExpect(jsonPath("$.[*].phaseImage").value(hasItem(DEFAULT_PHASE_IMAGE.toString())));
    }

    @Test
    @Transactional
    public void getPhase() throws Exception {
        // Initialize the database
        phaseRepository.saveAndFlush(phase);

        // Get the phase
        restPhaseMockMvc.perform(get("/api/phases/{id}", phase.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(phase.getId().intValue()))
            .andExpect(jsonPath("$.phaseName").value(DEFAULT_PHASE_NAME.toString()))
            .andExpect(jsonPath("$.phaseDescription").value(DEFAULT_PHASE_DESCRIPTION.toString()))
            .andExpect(jsonPath("$.phaseImage").value(DEFAULT_PHASE_IMAGE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingPhase() throws Exception {
        // Get the phase
        restPhaseMockMvc.perform(get("/api/phases/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updatePhase() throws Exception {
        // Initialize the database
        phaseService.save(phase);

        int databaseSizeBeforeUpdate = phaseRepository.findAll().size();

        // Update the phase
        Phase updatedPhase = phaseRepository.findOne(phase.getId());
        updatedPhase
                .phaseName(UPDATED_PHASE_NAME)
                .phaseDescription(UPDATED_PHASE_DESCRIPTION);

        restPhaseMockMvc.perform(put("/api/phases")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedPhase)))
                .andExpect(status().isOk());

        // Validate the Phase in the database
        List<Phase> phases = phaseRepository.findAll();
        assertThat(phases).hasSize(databaseSizeBeforeUpdate);
        Phase testPhase = phases.get(phases.size() - 1);
        assertThat(testPhase.getPhaseName()).isEqualTo(UPDATED_PHASE_NAME);
        assertThat(testPhase.getPhaseDescription()).isEqualTo(UPDATED_PHASE_DESCRIPTION);
    }

    @Test
    @Transactional
    public void deletePhase() throws Exception {
        // Initialize the database
        phaseService.save(phase);

        int databaseSizeBeforeDelete = phaseRepository.findAll().size();

        // Get the phase
        restPhaseMockMvc.perform(delete("/api/phases/{id}", phase.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Phase> phases = phaseRepository.findAll();
        assertThat(phases).hasSize(databaseSizeBeforeDelete - 1);
    }
}
