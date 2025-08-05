package dev.vanadium.quasarplatform.bpmn.parser

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class XmlElement {

    @JacksonXmlProperty(isAttribute = true)
    lateinit var name: String

    lateinit var text: String

    @JacksonXmlElementWrapper(useWrapping = false)
    var children: List<XmlElement> = arrayListOf()

    var attributes: Map<String, String> = hashMapOf()
}