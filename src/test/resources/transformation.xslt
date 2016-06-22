<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs" version="2.0">
    <xsl:output indent="yes" omit-xml-declaration="no" doctype-public="text/xml" /> 
    
    <xsl:template match="/" >
        <xsl:apply-templates />       
    </xsl:template>
    
    <xsl:template match="node()">
        <![CDATA[
        ]]>
        <xsl:choose>
            <xsl:when test="local-name() = 'element' and @type ">
                <xsl:element name="element" namespace="http://www.w3.org/2001/XMLSchema">
                    <xsl:for-each select="@*">
                        <xsl:if test="local-name()!='type' ">
                           <xsl:attribute name="{local-name()}"><xsl:value-of select="."/></xsl:attribute>
                        </xsl:if>
                    </xsl:for-each>
                   <xs:complexType>
                       <xs:complexContent>
                           <xs:restriction base="{@type}"/>
                       </xs:complexContent>
                   </xs:complexType>
                </xsl:element>
            </xsl:when>
            <xsl:when test="not(local-name())  ">
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="name()"/>
                <xsl:element name="{name()}" namespace="{namespace-uri()}">
                    <xsl:for-each select="@*">
                         <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
                    </xsl:for-each>
                    <xsl:apply-templates />
                    
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>