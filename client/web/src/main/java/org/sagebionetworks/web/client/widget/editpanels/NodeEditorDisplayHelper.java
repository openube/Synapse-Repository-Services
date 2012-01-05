package org.sagebionetworks.web.client.widget.editpanels;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.ontology.StaticEnumerations;
import org.sagebionetworks.web.shared.NodeType;

import com.google.inject.Inject;

public class NodeEditorDisplayHelper {	
	
	private Map<NodeType, SpecificNodeTypeDeviation> typeToDevaition;
		
	@Inject
	public NodeEditorDisplayHelper() {		
		typeToDevaition = new HashMap<NodeType, SpecificNodeTypeDeviation>();		
		typeToDevaition.put(NodeType.DATASET, createDatasetDeviation());
		typeToDevaition.put(NodeType.LAYER, createLayerDeviation());
		typeToDevaition.put(NodeType.PROJECT, createProjectDeviation());		
		typeToDevaition.put(NodeType.ANALYSIS, createAnalysisDeviation());		
		typeToDevaition.put(NodeType.STEP, createStepDeviation());		
	}

	public SpecificNodeTypeDeviation getNodeTypeDeviation(NodeType type) {
		if(typeToDevaition.containsKey(type)) {
			return typeToDevaition.get(type);
		}
		return null;
	}
			
	/*
	 * Private Methods
	 */
	// DATASET
	private SpecificNodeTypeDeviation createDatasetDeviation() {
		SpecificNodeTypeDeviation deviation = new SpecificNodeTypeDeviation(
				NodeType.DATASET, "Dataset",
				DisplayConstants.CREATE_DATASET_TEXT,
				DisplayConstants.EDIT_DATASET_TEXT,
				Arrays.asList(new String[] { "name" }),
				Arrays.asList(new String[] { "versionLabel", "releaseDate",
						"description", "name", "status" }));
		deviation.setKeyToOntology(new StaticEnumerations().getAnnotationToEnum());			
		return deviation;
	}	
	
	// LAYER
	private SpecificNodeTypeDeviation createLayerDeviation() {
		SpecificNodeTypeDeviation deviation = new SpecificNodeTypeDeviation(
				NodeType.LAYER, "Layer", DisplayConstants.CREATE_LAYER_TEXT,
				DisplayConstants.EDIT_LAYER_TEXT,
 Arrays.asList(new String[] { "name", "type" }),
				Arrays.asList(new String[] {
						"releaseNotes", "tissueType", "type",
						"description", "name", "publicationDate", "qcBy",
						"platform", "status", 
						"processingFacility", "numSamples", "qcDate",
						"versionNumber" }));
		deviation.setKeyToOntology(new StaticEnumerations().getAnnotationToEnum());			
		return deviation;
	}
	
	// PROJECT
	private SpecificNodeTypeDeviation createProjectDeviation() {
		SpecificNodeTypeDeviation deviation = new SpecificNodeTypeDeviation(
				NodeType.PROJECT, "Project",
				DisplayConstants.CREATE_PROJECT_TEXT,
				DisplayConstants.EDIT_PROJECT_TEXT, 
				Arrays.asList(new String[] { "name" }),
				Arrays.asList(new String[] { "status", "version", "description", "name" }));
		deviation.setKeyToOntology(new StaticEnumerations().getAnnotationToEnum());
			
		return deviation;
	}
	
	// ANALYSIS
	private SpecificNodeTypeDeviation createAnalysisDeviation() {
		SpecificNodeTypeDeviation deviation = new SpecificNodeTypeDeviation(
				NodeType.ANALYSIS, "Analysis",
				DisplayConstants.CREATE_ANALYSIS_TEXT,
				DisplayConstants.EDIT_ANALYSIS_TEXT, 
				Arrays.asList(new String[] {"name"}),
				Arrays.asList(new String[] {"name","description"}));		
		deviation.setKeyToOntology(new StaticEnumerations().getAnnotationToEnum());
			
		return deviation;
	}
	
	// STEP
	private SpecificNodeTypeDeviation createStepDeviation() {
		SpecificNodeTypeDeviation deviation = new SpecificNodeTypeDeviation(
				NodeType.STEP, "Step",
				DisplayConstants.CREATE_STEP_TEXT,
				DisplayConstants.EDIT_STEP_TEXT, 
				Arrays.asList(new String[] {"name"}),
				Arrays.asList(new String[] {"name", "description"}));		
		deviation.setKeyToOntology(new StaticEnumerations().getAnnotationToEnum());
			
		return deviation;
	}
	
	
}