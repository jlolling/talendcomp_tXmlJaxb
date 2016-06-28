<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.0">
    <xsl:output indent="yes" omit-xml-declaration="no" doctype-public="text/xml" /> 
	<!--  get targetNs of current schema -->    
    <xsl:variable name="targetNs"><xsl:value-of select="/descendant-or-self::node()[local-name()='schema']/@targetNamespace" /></xsl:variable>
	
	<xsl:template name="resolvePrefix">
		<xsl:param name="prefix" />
		<xsl:for-each select="/descendant-or-self::node()[local-name()='schema']/namespace::*">
			<xsl:if test="name()=$prefix">
				<xsl:value-of select="." />
			</xsl:if>		
		</xsl:for-each>
	</xsl:template>
	
    <xsl:template match="/" >
    	<xsl:value-of select="$targetNs"/>
    	<xsl:variable  name="t1">
    	<xsl:call-template name="resolvePrefix"/>
    	</xsl:variable>
    	t1=<xsl:value-of select="$t1"></xsl:value-of>
    	
    	 
  	    	<xsl:variable name="t2"><xsl:call-template name="resolvePrefix" >
  	    	<xsl:with-param name="prefix" value="'xsd'"/></xsl:call-template></xsl:variable>
    	  
    	t2=<xsl:value-of select="$t2"></xsl:value-of>
  	    	
        <xsl:apply-templates />       
    </xsl:template>
    
    <xsl:template match="node()">
        <![CDATA[
        ]]>
        <xsl:choose>
            <xsl:when test="local-name() = 'element' and @type">
        		<xsl:variable name="type"><xsl:value-of select="@type" /></xsl:variable>
        			<xsl:variable name="fragment">
	        			<xsl:choose>
	        				<xsl:when test="not(contains($type, ':'))">
								<xsl:when test="not($targetNs)">
									
	        					</xsl:when>
        					    <xsl:otherwise>
							        <xsl:value-of select="other" />
							    </xsl:otherwise>
	        				</xsl:when>
	        			</xsl:choose>
        			</xsl:variable>
        			
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