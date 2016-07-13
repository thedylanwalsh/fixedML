    <!--
        Copyright 2016 Dylan Walsh.

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see &lt;http://www.gnu.org/licenses/>-->
    <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
        <xsl:output method="html" indent="yes" doctype-system="about:legacy-compat"/>
        <xsl:strip-space elements="*"/>

        <xsl:template match="/people">
            <html>
                <head><title>fixedML - fixed width - 2 records as XHTML using XSLT.</title></head>
                <body><xsl:apply-templates select="person"/></body>
            </html>
        </xsl:template>

        <xsl:template match="person">
            <div id="person_{personId}">
                <div class="name"><xsl:value-of select="firstName"/>&#xA0;<xsl:value-of select="lastName"/></div>
                <xsl:for-each select="phone">
                    <div class="phone"><xsl:value-of select="phoneNumber"/></div>
                </xsl:for-each>
                <xsl:for-each select="address">
                    <div class="addressline"><xsl:value-of select="line1"/></div>
                    <div class="addressline"><xsl:value-of select="line2"/></div>
                    <div class="addressline"><xsl:value-of select="line3"/></div>
                    <div class="addressline"><xsl:value-of select="line4"/></div>
                    <div class="addressline"><xsl:value-of select="line5"/></div>
                    <div class="postalCode"><xsl:value-of select="postalCode"/></div>
                    <div class="city"><xsl:value-of select="city"/></div>
                    <div class="countryIso"><xsl:value-of select="countryIso"/></div>
                </xsl:for-each>
            </div>
        </xsl:template>
    </xsl:stylesheet>