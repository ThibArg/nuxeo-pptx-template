package nuxeo.pptx.template;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.fm.FMContextBuilder;
import org.nuxeo.template.processors.xdocreport.XDocReportBindingResolver;

import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;
/**
 * ASSUME nuxeo-template-renderng is installed
 */
@Operation(id=PPTXTemplateProcessor.ID, category=Constants.CAT_DOCUMENT, label="PPTX Template: Process", description="Describe here what your operation does.")
public class PPTXTemplateProcessor {

    public static final String ID = "Document.PPTXTemplateProcessor";
    
    private static final Log log = LogFactory.getLog(PPTXTemplateProcessor.class);
    
    @Context
    protected CoreSession session;

    @Param(name = "templateTitle", required = true)
    protected String templateTitle;

    @OperationMethod
    public Blob run(DocumentModel inDoc) throws IOException, XDocReportException {
        Blob blob = null;
        
        // Find the template
        String nxql = "SELECT * FROM TemplateSource WHERE dc:title = '" + templateTitle + "'";
        nxql += " AND ecm:isVersion = 0 AND ecm:isProxy = 0 AND ecm:currentLifeCycleState != 'deleted'";
        DocumentModelList docs = session.query(nxql);
        if(docs.size() == 0) {
            log.error("Template " + templateTitle + " not found");
        }
        
        DocumentModel templateDoc = docs.get(0);

        // Get template file
        Blob templateFile = (Blob) templateDoc.getPropertyValue("file:content");
        InputStream in = templateFile.getStream();
        
        IXDocReport report;
        report = XDocReportRegistry.getRegistry().loadReport( in, TemplateEngineKind.Freemarker, false );
        
        TemplateSourceDocument tmplSrceDoc = templateDoc.getAdapter(TemplateSourceDocument.class);
        List<TemplateInput> params = tmplSrceDoc.getParams();
        FieldsMetadata metadata = new FieldsMetadata();
        for (TemplateInput param : params) {
            if (param.getType() == InputType.PictureProperty) {
                metadata.addFieldAsImage(param.getName());
            }
        }
        report.setFieldsMetadata(metadata);
        
        inDoc.addFacet("TemplateBased");
        TemplateBasedDocument templateBasedDocument = inDoc.getAdapter(TemplateBasedDocument.class);
        templateBasedDocument.setTemplate(templateDoc,false);
        
        FMContextBuilder fmContextBuilder = new FMContextBuilder();
        Map<String, Object> ctx = fmContextBuilder.build(inDoc, "templatePPTX");

        XDocReportBindingResolver resolver = new XDocReportBindingResolver(metadata);
        resolver.resolve(params, ctx, templateBasedDocument);
        
        IContext context = report.createContext();
        for (String key : ctx.keySet()) {
            context.put(key, ctx.get(key));
        }
        
        blob = Blobs.createBlobWithExtension(".pptx");
        OutputStream out = new FileOutputStream( blob.getFile() );
        report.process( context, out );
        
        
        // Force reload the document because we changed it (addFacet)
        inDoc.removeFacet("TemplateBased");
        inDoc.refresh();
        
        return blob;
    }
}
