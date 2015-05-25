package org.topbraid.shacl.vocabulary;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.Var;

/**
 * Vocabulary for http://www.w3.org/ns/shacl
 * 
 * @author Holger Knublauch
 */
public class SH {

    public final static String BASE_URI = "http://www.w3.org/ns/shacl";
    
    public final static String NAME = "SHACL";

    public final static String NS = BASE_URI + "#";

    public final static String PREFIX = "sh";


    public final static Resource AbstractPropertyConstraint = ResourceFactory.createResource(NS + "AbstractPropertyConstraint");

    public final static Resource Argument = ResourceFactory.createResource(NS + "Argument");

    public final static Resource BlankNode = ResourceFactory.createResource(NS + "BlankNode");
    
    public final static Resource Constraint = ResourceFactory.createResource(NS + "Constraint");

    public final static Resource ConstraintTemplate = ResourceFactory.createResource(NS + "ConstraintTemplate");
    
    public final static Resource ConstraintViolation = ResourceFactory.createResource(NS + "ConstraintViolation");

    public final static Resource CountConstraint = ResourceFactory.createResource(NS + "CountConstraint");

    public final static Resource Error = ResourceFactory.createResource(NS + "Error");

    public final static Resource FatalError = ResourceFactory.createResource(NS + "FatalError");

    public final static Resource Function = ResourceFactory.createResource(NS + "Function");

    public final static Resource Functions = ResourceFactory.createResource(NS + "Functions");

    public final static Resource Graph = ResourceFactory.createResource(NS + "Graph");

    public final static Resource InversePropertyConstraint = ResourceFactory.createResource(NS + "InversePropertyConstraint");

    public final static Resource IRI = ResourceFactory.createResource(NS + "IRI");

    public final static Resource Literal = ResourceFactory.createResource(NS + "Literal");

    public final static Resource Macro = ResourceFactory.createResource(NS + "Macro");
    
    public final static Resource NativeConstraint = ResourceFactory.createResource(NS + "NativeConstraint");
    
    public final static Resource OrConstraint = ResourceFactory.createResource(NS + "OrConstraint");

    public final static Resource PropertyConstraint = ResourceFactory.createResource(NS + "PropertyConstraint");

    public final static Resource Shape = ResourceFactory.createResource(NS + "Shape");

    public final static Resource ShapeClass = ResourceFactory.createResource(NS + "ShapeClass");

    public final static Resource Template = ResourceFactory.createResource(NS + "Template");
    
    public final static Resource TemplateConstraint = ResourceFactory.createResource(NS + "TemplateConstraint");

    public final static Resource Templates = ResourceFactory.createResource(NS + "Templates");

    public final static Resource Warning = ResourceFactory.createResource(NS + "Warning");

    
    public final static Property abstract_ = ResourceFactory.createProperty(NS + "abstract");

    public final static Property allowedValues = ResourceFactory.createProperty(NS + "allowedValues");

    public final static Property argument = ResourceFactory.createProperty(NS + "argument");

    public final static Property cachable = ResourceFactory.createProperty(NS + "cachable");

    public final static Property constraint = ResourceFactory.createProperty(NS + "constraint");

    public final static Property datatype = ResourceFactory.createProperty(NS + "datatype");

    public final static Property defaultValue = ResourceFactory.createProperty(NS + "defaultValue");

    public final static Property defaultValueType = ResourceFactory.createProperty(NS + "defaultValueType");

    public final static Property graph = ResourceFactory.createProperty(NS + "graph");

    public final static Resource hasShape = ResourceFactory.createResource(NS + "hasShape");

    public final static Resource hasType = ResourceFactory.createResource(NS + "hasType");

    public final static Property hasValue = ResourceFactory.createProperty(NS + "hasValue");
    
    public final static Property include = ResourceFactory.createProperty(NS + "include");

    public final static Property inverse = ResourceFactory.createProperty(NS + "inverse");

    public final static Property inverseProperty = ResourceFactory.createProperty(NS + "inverseProperty");

    public final static Property labelTemplate = ResourceFactory.createProperty(NS + "labelTemplate");

    public final static Property maxCount = ResourceFactory.createProperty(NS + "maxCount");

    public final static Property member = ResourceFactory.createProperty(NS + "member");

    public final static Property message = ResourceFactory.createProperty(NS + "message");

    public final static Property minCount = ResourceFactory.createProperty(NS + "minCount");

    public final static Property nodeShape = ResourceFactory.createProperty(NS + "nodeShape");

    public final static Property nodeKind = ResourceFactory.createProperty(NS + "nodeKind");

    public final static Property object = ResourceFactory.createProperty(NS + "object");

    public final static Resource objectCount = ResourceFactory.createResource(NS + "objectCount");

    public final static Property optional = ResourceFactory.createProperty(NS + "optional");

    public final static Property optionalWhenInherited = ResourceFactory.createProperty(NS + "optionalWhenInherited");

    public final static Property predicate = ResourceFactory.createProperty(NS + "predicate");

    public final static Property property = ResourceFactory.createProperty(NS + "property");

    public final static Property returnType = ResourceFactory.createProperty(NS + "returnType");

    public final static Property root = ResourceFactory.createProperty(NS + "root");
    
    public final static Property scopeClass = ResourceFactory.createProperty(NS + "scopeClass");
    
    public final static Property scopeShape = ResourceFactory.createProperty(NS + "scopeShape");
    
    public final static Property severity = ResourceFactory.createProperty(NS + "severity");
    
    public final static Property shapes = ResourceFactory.createProperty(NS + "shapes");

    public final static Property shapesGraph = ResourceFactory.createProperty(NS + "shapesGraph");

    public final static Property source = ResourceFactory.createProperty(NS + "source");

    public final static Property sparql = ResourceFactory.createProperty(NS + "sparql");

    public final static Property sparqlEntailment = ResourceFactory.createProperty(NS + "sparqlEntailment");

    public final static Property subject = ResourceFactory.createProperty(NS + "subject");
    
    public final static Property valueShape = ResourceFactory.createProperty(NS + "valueShape");

    public final static Property valueType = ResourceFactory.createProperty(NS + "valueType");

	
	public static final Var currentShapeVar = Var.alloc("currentShape");

	public static final Var messageVar = Var.alloc(message.getLocalName());

	public static final Var objectVar = Var.alloc(object.getLocalName());

	public static final Var predicateVar = Var.alloc(predicate.getLocalName());

	public static final Var rootVar = Var.alloc(root.getLocalName());

	public static final Var severityVar = Var.alloc(severity.getLocalName());
	
	public static final Var shapesGraphVar = Var.alloc("shapesGraph");

	public static final Var subjectVar = Var.alloc(subject.getLocalName());
	
	public static final Var thisVar = Var.alloc("this");


    public static String getURI() {
        return NS;
    }
}
