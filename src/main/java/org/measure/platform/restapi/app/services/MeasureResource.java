package org.measure.platform.restapi.app.services;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;

import org.measure.platform.core.api.IMeasureCatalogueService;
import org.measure.platform.core.api.IRemoteCatalogueService;
import org.measure.platform.restapi.framework.rest.util.HeaderUtil;
import org.measure.smm.measure.model.SMMMeasure;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;


@RestController
@RequestMapping(value = "api/measure")
public class MeasureResource {	
	
	@Inject
	IMeasureCatalogueService measureCatalogue;
	
	@Inject
	IRemoteCatalogueService remoteCatalogue;
	
	@RequestMapping(value = "/upload", method = RequestMethod.PUT)
	public void handleFileUpload(@RequestBody String data) {
		try {
			  byte[] fileData = DatatypeConverter.parseBase64Binary(data.substring(data.indexOf(",") + 1));	
			  File tempFile = File.createTempFile(data.substring(0, 20), "measure");		  
		      BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(tempFile));
		      stream.write(fileData);
		      stream.close();		      
		      measureCatalogue.storeMeasure(tempFile.toPath());
		      tempFile.delete();		      
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    /**
     * GET  /measures : get all the measures.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of projects in body
     */
    @GetMapping("/findall")
    @Timed
    public List<SMMMeasure> getAllMeasures() {
    	List<SMMMeasure> result = new ArrayList<>();
    	result.addAll(this.measureCatalogue.getAllMeasures());
    	result.addAll(this.remoteCatalogue.getAllMeasures());
        return result;
    }
    
    /**
     * GET  /measure/:id : get the "id" measure.
     *
     * @param id the id of the measure to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the measure, or with status 404 (Not Found)
     */
    @GetMapping("/{id}")
    @Timed
    public ResponseEntity<SMMMeasure> getMeasure(@PathVariable String id) {
    	System.out.println("GET " + id);
        SMMMeasure measure = measureCatalogue.getMeasure(id);
        return Optional.ofNullable(measure)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    /**
     * DELETE  /measure/:id : delete the "id" measure.
     *
     * @param id the id of the measure to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/{id}")
    @Timed
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
    	measureCatalogue.deleteMeasure(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("measure", id)).build();
    }
}