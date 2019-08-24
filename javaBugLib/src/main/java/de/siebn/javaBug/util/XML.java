package de.siebn.javaBug.util;

import org.w3c.dom.*;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XML {
    protected Document document;
    protected Element element;

    public XML() {
        this("html");
    }

    public XML(String root) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            document = factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        element = document.createElement(root);
        document.appendChild(element);
    }

    public XML(InputStream in) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            document = factory.newDocumentBuilder().parse(in);
            element = (Element) document.getFirstChild();
            Stack<NodeList> nodeLists = new Stack<>();
            nodeLists.add(document.getChildNodes());
            while (!nodeLists.isEmpty()) {
                NodeList list = nodeLists.pop();
                for (int i = 0; i < list.getLength(); i++) {
                    Node node = list.item(i);
                    nodeLists.add(node.getChildNodes());
                    if (node instanceof Element) {
                        Element e = (Element) node;
                        if (e.hasAttribute("id"))
                            e.setIdAttribute("id", true);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected XML(Document document, String tagName) {
        this.document = document;
        element = document.createElement(tagName);
    }

    protected XML(Document document, Element element) {
        this.document = document;
        this.element = element;
    }

    public XML getById(String id) {
        return new XML(document, document.getElementById(id));
    }

    public XML getFirstByTag(String tag) {
        NodeList elements = element.getElementsByTagName(tag);
        return elements.getLength() == 0 ? null : new XML(document, (Element) elements.item(0));
    }

    public XML setAttr(String name, String value) {
        element.setAttribute(name, value);
        if (name.toLowerCase().equals("id"))
            element.setIdAttribute(name, true);
        return this;
    }

    public XML setId(String id) {
        element.setAttribute("id", id);
        element.setIdAttribute("id", true);
        return this;
    }

    public String getId() {
        return element.getAttribute("id");
    }

    public XML setClass(String clazz) {
        element.setAttribute("class", clazz);
        return this;
    }

    public XML addClass(String clazz) {
        String oldClasses = element.getAttribute("class");
        setClass(oldClasses == null ? clazz : oldClasses + " " + clazz);
        return this;
    }

    public XML setHref(String href) {
        element.setAttribute("href", href);
        return this;
    }

    public XML appendText(String text) {
        element.appendChild(document.createTextNode(text));
        return this;
    }

    public XML addElement(XML e) {
        if (!element.getOwnerDocument().equals(e.element.getOwnerDocument()))
            element.appendChild(document.importNode(e.element, true));
        else
            element.appendChild(e.element);
        return e;
    }

    public XML addElement(XML e, int pos) {
        NodeList nodes = element.getChildNodes();
        if (pos < nodes.getLength())
            element.insertBefore(e.element, nodes.item(pos));
        else
            addElement(e);
        return e;
    }

    public XML add(String tag) {
        return addElement(new XML(document, tag));
    }

    public XML add(String tag, int pos) {
        return addElement(new XML(document, tag), pos);
    }

    public String convertToXml(String prefix, Node root) {
        try {
            StringWriter sw = new StringWriter();
            if (prefix != null) sw.write(prefix);
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "no");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.METHOD, "html");
            if (root == null) {
                t.transform(new DOMSource(document), new StreamResult(sw));
            } else {
                NodeList children = root.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    t.transform(new DOMSource(children.item(i)), new StreamResult(sw));
                }
            }
            sw.close();
            return sw.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getXml() {
        return convertToXml(null, null);
    }

    public String getChildrenXml() {
        return convertToXml(null, element);
    }

    public static class HTML extends XML {
        public String getHtml() {
            return convertToXml("<!DOCTYPE html>\n", null);
        }
    }
}
