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
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.07">
    <xsl:output indent="no" method="xml" encoding="UTF-8" media-type="text/xml"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="/people">
        <dw:Cdtrs xmlns:dw="http://dylanwalsh.net/fixedML">
            <xsl:apply-templates select="person"/>
        </dw:Cdtrs>
    </xsl:template>

    <xsl:template match="person">
        <Cdtr>
            <Nm><xsl:value-of select="firstName"/>&#xA0;<xsl:value-of select="lastName"/></Nm>
            <xsl:for-each select="address[1]">
                <PstlAdr>
                    <StrtNm><xsl:value-of select="line2"/></StrtNm>
                    <BldgNb><xsl:value-of select="line1"/></BldgNb>
                    <PstCd><xsl:value-of select="postalCode"/></PstCd>
                    <TwnNm><xsl:value-of select="city"/></TwnNm>
                    <Ctry><xsl:value-of select="countryIso"/></Ctry>
                </PstlAdr>
            </xsl:for-each>
            <CtctDtls>
                <xsl:choose>
                    <xsl:when test="phone[phoneNumberType='WORK']">
                        <PhneNb><xsl:value-of select="phone[phoneNumberType='WORK' and position() = 1]/phoneNumber"/></PhneNb>
                    </xsl:when>
                    <xsl:when test="phone[phoneNumberType='HOME']">
                        <PhneNb><xsl:value-of select="phone[phoneNumberType='HOME' and position() = 1]/phoneNumber"/></PhneNb>
                    </xsl:when>
                </xsl:choose>
                <xsl:if test="phone[phoneNumberType='MOBILE']">
                    <MobNb><xsl:value-of select="phone[phoneNumberType='MOBILE' and position() = 1]/phoneNumber"/></MobNb>
                </xsl:if>
            </CtctDtls>
        </Cdtr>
    </xsl:template>
</xsl:transform>