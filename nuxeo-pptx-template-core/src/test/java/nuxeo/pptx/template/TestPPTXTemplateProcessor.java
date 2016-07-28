package nuxeo.pptx.template;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({"org.nuxeo.template.manager.api",
    "org.nuxeo.template.manager",
    "org.nuxeo.template.manager.xdocreport",
    "nuxeo.pptx.template.nuxeo-pptx-template-core"})
public class TestPPTXTemplateProcessor {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;
    
    protected DocumentModel pptxTemplate;
    
    @Before
    public void startup() {
        
        // Create template
        pptxTemplate = session.createDocumentModel("/", "templatePPTX", "TemplateSource");
        File file = new File(getClass().getResource("/files/template.pptx").getPath());
        Blob blob = new FileBlob(file);
        pptxTemplate.setPropertyValue("file:content", (Serializable) blob);
        pptxTemplate.setPropertyValue("dc:title", "templatePPTX");
        String parameters = "<nxdt:templateParams xmlns:nxdt=\"http://www.nuxeo.org/DocumentTemplate\"><nxdt:field name=\"theDesc\" type=\"source\" source=\"dc:description\"></nxdt:field></nxdt:templateParams>";
        //parameters += "<nxdt:templateParams xmlns:nxdt=\"http://www.nuxeo.org/DocumentTemplate\"><nxdt:field name=\"theFormat\" type=\"source\" source=\"dc:format\"></nxdt:field></nxdt:templateParams>";
        //pptxTemplate.setPropertyValue("tmpl:templateData", parameters);
        pptxTemplate = session.createDocument(pptxTemplate);
        
        TemplateSourceDocument tmplSrceDoc = pptxTemplate.getAdapter(TemplateSourceDocument.class);
        tmplSrceDoc.initTemplate(true);
        
        TemplateInput ti = new TemplateInput("theDesc");
        ti.setType(InputType.DocumentProperty);
        ti.setSource("dc:description");
        tmplSrceDoc.addInput(ti);
        
        ti = new TemplateInput("theFormat");
        ti.setType(InputType.DocumentProperty);
        ti.setSource("dc:format");
        tmplSrceDoc.addInput(ti);
        
        ti = new TemplateInput("theDate");
        ti.setType(InputType.DocumentProperty);
        ti.setSource("dc:created");
        tmplSrceDoc.addInput(ti);
        
        pptxTemplate = session.saveDocument(pptxTemplate);

        
        session.save();
        
    }

    @Test
    public void shouldCallWithParameters() throws OperationException, IOException {
        
        DocumentModel doc = session.createDocumentModel("/", "theDoc", "File");
        doc.setPropertyValue("dc:title", "theDoc");
        doc.setPropertyValue("dc:description", "Some description");
        doc.setPropertyValue("dc:format", "Here, the format");
        doc = session.createDocument(doc);
               
        session.save();
        
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = new HashMap<>();
        params.put("templateTitle", "templatePPTX");
        Blob result = (Blob) automationService.run(ctx, PPTXTemplateProcessor.ID, params);
        
        if(result != null) {
            File destFile = new File("/Users/thibaud/Desktop/testTEMP/coucou.pptx");
            FileUtils.copyFile(result.getFile(), destFile);
        }
        
        assertNotNull(result);
    }
}
