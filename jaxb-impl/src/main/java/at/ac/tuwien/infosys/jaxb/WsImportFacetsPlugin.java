package at.ac.tuwien.infosys.jaxb;

import javax.xml.bind.annotation.Annotation;
import javax.xml.bind.annotation.AppInfo;
import javax.xml.bind.annotation.Documentation;
import javax.xml.bind.annotation.Facets;
import javax.xml.bind.annotation.Facets.WhiteSpace;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.generator.bean.ClassOutlineImpl;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIDeclaration;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIXPluginCustomization;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BindInfo;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.impl.AnnotationImpl;
import com.sun.xml.xsom.impl.ParticleImpl;

/**
 * wsimport plugin to generate JAXB-Facets specific annotations in generated Java code.
 * @author Waldemar Hummer (hummer@infosys.tuwien.ac.at)
 * @since JAXB-Facets 1.1.0
 */
public class WsImportFacetsPlugin extends Plugin {

    public String getOptionName() {
        return "jaxb-facets";
    }

    public String getUsage() {
        return "  -jaxb-facets    :  Generate JAXB annotations for XSD <facet>'s and <annotation>'s";
    }

    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler)
            throws SAXException {
        for(ClassOutline c: outline.getClasses()) {
            ClassOutlineImpl ci = (ClassOutlineImpl)c;
            XSComponent schemaEl = ci.target.getSchemaComponent();

            addXsdAnnotation(ci, schemaEl);
            addXsdFacets(ci, schemaEl);
        }
        return true;
    }


    public static void addXsdFacets(ClassOutlineImpl ci, XSComponent schemaEl) {
        JDefinedClass clazz = ci.implClass;
        for(FieldOutline f : ci.getDeclaredFields()) {
            XSComponent schema = f.getPropertyInfo().getSchemaComponent();

            if(schema instanceof ParticleImpl) {
                ParticleImpl p = (ParticleImpl)schema;
                if(p.getTerm().isElementDecl()) {
	                XSType type = p.getTerm().asElementDecl().getType();
	                if(type.isSimpleType()) {
	                    XSSimpleType stype = type.asSimpleType();
	                    for(String fName : Constants.FACET_NAMES) {
	                        XSFacet fValue = stype.getFacet(fName);
	                        if(fValue != null) {
	                            String name = f.getPropertyInfo().getName(false);
	                            JFieldVar var = clazz.fields().get(name);
	                            JAnnotationUse anno = getAnnotation(var, Facets.class);

	                            Class<?> typeClazz = Constants.FACET_TYPES.get(fName);
	                            if(typeClazz == long.class) {
	                                anno.param(fValue.getName(), Long.parseLong(fValue.getValue().value));
	                            } else if(typeClazz == String.class) {
	                                anno.param(fValue.getName(), fValue.getValue().value);
	                            } else if(typeClazz == WhiteSpace.class) {
	                                anno.param(fValue.getName(), WhiteSpace.valueOf(fValue.getValue().value));
	                            } else if(typeClazz == String[].class) {
	                                // TODO for "enumeration" facet - is this needed?
	                            }
	                        }
	                    }
	                }
                } else {
                	// TODO - what to do in this case..?
                }
            }
        }
    }

    public static void addXsdAnnotation(ClassOutlineImpl ci, XSComponent schemaEl) {
        XSAnnotation anno = schemaEl.getAnnotation();
        if(anno != null) {
            AnnotationImpl annoImpl = (AnnotationImpl)anno;
            BindInfo annoInfo = (BindInfo)annoImpl.getAnnotation();
            if(annoInfo != null) {
            	/* add @Documentation annotation */
	            final String doc = annoInfo.getDocumentation();
	            JAnnotationUse jAnno = null;
	            if(doc != null) {
	                jAnno = getAnnotation(ci.implClass, Annotation.class);
	                JAnnotationUse annoUse = jAnno.annotationParam("documentation", Documentation.class);
	                annoUse.param("value", doc);
	            }

	            /* add @AppInfo annotation */
	            for(BIDeclaration decl : annoInfo.getDecls()) {
	            	if(decl instanceof BIXPluginCustomization) {
	            		if(jAnno == null) {
	            			jAnno = getAnnotation(ci.implClass, Annotation.class);
	            		}
	            		JAnnotationUse annoUseAppInfo = jAnno.annotationParam("appinfo", AppInfo.class);
	            		BIXPluginCustomization plc = (BIXPluginCustomization) decl;
	            		annoUseAppInfo.param("value", XmlUtil.toStringWithStrippedNamespaces(plc.element));
	            		/*
	            		 * TODO: <appinfo>'s source=".." attribute is not available due to limitations in JAXB/XJC:
	            		 * http://grepcode.com/file/repo1.maven.org/maven2/com.sun.xml.bind/jaxb-xjc/2.2.5/com/sun/tools/xjc/reader/xmlschema/bindinfo/BindInfo.java?av=f#148
	            		 */
	            		
	            	}
	            }
            }
        }
    }

    private static JAnnotationUse getAnnotation(JDefinedClass clazz, 
            Class<? extends java.lang.annotation.Annotation> annoClass) {
        for(JAnnotationUse a : clazz.annotations()) {
            if(a.getAnnotationClass().fullName().equals(annoClass.getName())) {
                return a;
            }
        }
        JAnnotationUse jAnno = clazz.annotate(annoClass);
        return jAnno;
    }

    private static JAnnotationUse getAnnotation(JFieldVar var, 
            Class<? extends java.lang.annotation.Annotation> annoClass) {
        for(JAnnotationUse a : var.annotations()) {
            if(a.getAnnotationClass().fullName().equals(annoClass.getName())) {
                return a;
            }
        }
        JAnnotationUse jAnno = var.annotate(annoClass);
        return jAnno;
    }

}
