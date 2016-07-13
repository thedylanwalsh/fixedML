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
<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output indent="no" omit-xml-declaration="yes" method="text" encoding="UTF-8" media-type="text/plain"/>
    <xsl:strip-space elements="*"/>

    <xsl:variable name="pad10" select="'          '"/>
    <xsl:variable name="pad20" select="'                    '"/>
    <xsl:variable name="pad30" select="'                              '"/>
    <xsl:variable name="pad50" select="'                                                  '"/>

    <xsl:template match="/people">000&#xA;001<xsl:value-of
            select="substring(concat($pad10, personCount/text()),
                       string-length(personCount/text()) + 1, 10)"/><xsl:value-of
            select="creationDate"/>&#xA;<xsl:apply-templates
            select="person"/>&#xA;999</xsl:template>

    <xsl:template match="person">&#xA;100&#xA;101<xsl:value-of
            select="personId"/><xsl:value-of
            select="substring(concat($pad30, firstName/text()),
                       string-length(firstName/text()) + 1, 30)"/><xsl:value-of
            select="substring(concat($pad30, lastName/text()),
                       string-length(lastName/text()) + 1, 30)"/>&#xA;200&#xA;<xsl:apply-templates
            select="address"/>&#xA;299&#xA;<xsl:apply-templates
            select="phone"/>&#xA;199</xsl:template>

    <xsl:template match="address">201<xsl:value-of
            select="addressId"/><xsl:value-of
            select="substring(concat($pad50, line1/text()),
                       string-length(line1/text()) + 1, 50)"/><xsl:value-of
            select="substring(concat($pad50, line2/text()),
                       string-length(line2/text()) + 1, 50)"/><xsl:value-of
            select="substring(concat($pad50, line3/text()),
                       string-length(line3/text()) + 1, 50)"/><xsl:value-of
            select="substring(concat($pad50, line4/text()),
                       string-length(line4/text()) + 1, 50)"/><xsl:value-of
            select="substring(concat($pad50, line5/text()),
                       string-length(line5/text()) + 1, 50)"/><xsl:value-of
            select="substring(concat($pad30, city/text()),
                       string-length(city/text()) + 1, 30)"/><xsl:value-of
            select="substring(concat($pad30, county/text()),
                       string-length(county/text()) + 1, 30)"/><xsl:value-of
            select="substring(concat($pad30, state/text()),
                       string-length(state/text()) + 1, 10)"/><xsl:value-of
            select="countryIso"/><xsl:value-of
            select="substring(concat($pad30, postalCode/text()),
                       string-length(postalCode/text()) + 1, 30)"/><xsl:value-of
            select="substring(concat($pad10, postalCodeType/text()),
                       string-length(postalCode/text()) + 1, 10)"/>&#xA;</xsl:template>

    <xsl:template match="phone">301<xsl:value-of
            select="substring(concat($pad20, phoneNumber),
                       string-length(phoneNumber) + 1, 20)"/><xsl:value-of
            select="substring(concat('      ', phoneNumberType/text()),
                       string-length(phoneNumberType/text()) + 1, 6)"/>&#xA;</xsl:template>

    <xsl:template match="*" />
</xsl:transform>