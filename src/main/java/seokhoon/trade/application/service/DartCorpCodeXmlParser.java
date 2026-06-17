package seokhoon.trade.application.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import seokhoon.trade.domain.research.DartCorpCodeRecord;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DartCorpCodeXmlParser {
    public List<DartCorpCodeRecord> parse(byte[] content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(
                    new StringReader(new String(content, StandardCharsets.UTF_8))));
            NodeList nodes = document.getElementsByTagName("list");
            List<DartCorpCodeRecord> records = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                records.add(new DartCorpCodeRecord(
                        text(element, "corp_code"),
                        text(element, "corp_name"),
                        text(element, "stock_code"),
                        text(element, "modify_date")
                ));
            }
            return records;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid DART corp code XML", exception);
        }
    }

    private static String text(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }
}
