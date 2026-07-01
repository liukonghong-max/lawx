package com.law4x.rag.infrastructure.dependency;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class AgentScopeDependencyTest {

    @Test
    void usesAgentScopeV2VersionForSimpleRagExtension() throws Exception {
        Document pom = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile());

        String agentScopeVersion = textOfFirst(pom, "agentscope.version");
        Element simpleRagDependency = findDependency(pom, "io.agentscope", "agentscope-extensions-rag-simple");

        assertThat(agentScopeVersion).isEqualTo("2.0.0-RC3");
        assertThat(textOf(simpleRagDependency, "version")).isEqualTo("${agentscope.version}");
    }

    private static Element findDependency(Document pom, String groupId, String artifactId) {
        NodeList dependencies = pom.getElementsByTagName("dependency");
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            if (groupId.equals(textOf(dependency, "groupId"))
                    && artifactId.equals(textOf(dependency, "artifactId"))) {
                return dependency;
            }
        }
        throw new AssertionError("Dependency not found: " + groupId + ":" + artifactId);
    }

    private static String textOfFirst(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            throw new AssertionError("Tag not found: " + tagName);
        }
        return nodes.item(0).getTextContent().trim();
    }

    private static String textOf(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }
}
