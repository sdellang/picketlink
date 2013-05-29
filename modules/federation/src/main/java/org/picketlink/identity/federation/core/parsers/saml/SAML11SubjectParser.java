/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.picketlink.identity.federation.core.parsers.saml;

import org.picketlink.common.PicketLinkLogger;
import org.picketlink.common.PicketLinkLoggerFactory;
import org.picketlink.common.constants.JBossSAMLConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ParsingException;
import org.picketlink.common.parsers.ParserNamespaceSupport;
import org.picketlink.common.util.StaxParserUtil;
import org.picketlink.identity.federation.core.parsers.util.SAML11ParserUtil;
import org.picketlink.identity.federation.core.saml.v1.SAML11Constants;
import org.picketlink.identity.federation.saml.v1.assertion.SAML11NameIdentifierType;
import org.picketlink.identity.federation.saml.v1.assertion.SAML11SubjectConfirmationType;
import org.picketlink.identity.federation.saml.v1.assertion.SAML11SubjectType;
import org.picketlink.identity.federation.saml.v1.assertion.SAML11SubjectType.SAML11SubjectTypeChoice;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.net.URI;

/**
 * Parse the saml subject
 *
 * @author Anil.Saldhana@redhat.com
 * @since Oct 12, 2010
 */
public class SAML11SubjectParser implements ParserNamespaceSupport {
    
    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();
    
    /**
     * @see {@link ParserNamespaceSupport#parse(XMLEventReader)}
     */
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        StaxParserUtil.getNextEvent(xmlEventReader);

        SAML11SubjectType subject = new SAML11SubjectType();

        // Peek at the next event
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = (EndElement) xmlEvent;
                if (StaxParserUtil.matches(endElement, JBossSAMLConstants.SUBJECT.get())) {
                    endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                    break;
                } else
                    throw logger.parserUnknownEndElement(StaxParserUtil.getEndElementName(endElement));
            }

            StartElement peekedElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (peekedElement == null)
                break;

            String tag = StaxParserUtil.getStartElementName(peekedElement);

            if (SAML11Constants.NAME_IDENTIFIER.equalsIgnoreCase(tag)) {
                peekedElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                String val = StaxParserUtil.getElementText(xmlEventReader);
                SAML11NameIdentifierType nameID = new SAML11NameIdentifierType(val);
                Attribute formatAtt = peekedElement.getAttributeByName(new QName(SAML11Constants.FORMAT));
                if (formatAtt != null) {
                    nameID.setFormat(URI.create(StaxParserUtil.getAttributeValue(formatAtt)));
                }

                Attribute nameQAtt = peekedElement.getAttributeByName(new QName(SAML11Constants.NAME_QUALIFIER));
                if (nameQAtt != null) {
                    nameID.setNameQualifier(StaxParserUtil.getAttributeValue(nameQAtt));
                }

                SAML11SubjectTypeChoice subChoice = new SAML11SubjectTypeChoice(nameID);
                subject.setChoice(subChoice);
            } else if (JBossSAMLConstants.SUBJECT_CONFIRMATION.get().equalsIgnoreCase(tag)) {
                SAML11SubjectConfirmationType subjectConfirmationType = SAML11ParserUtil
                        .parseSAML11SubjectConfirmation(xmlEventReader);
                subject.setSubjectConfirmation(subjectConfirmationType);
            } else
                throw logger.parserUnknownTag(tag, peekedElement.getLocation());
        }
        return subject;
    }

    /**
     * @see {@link ParserNamespaceSupport#supports(QName)}
     */
    public boolean supports(QName qname) {
        String nsURI = qname.getNamespaceURI();
        String localPart = qname.getLocalPart();

        return nsURI.equals(JBossSAMLURIConstants.ASSERTION_NSURI.get()) && localPart.equals(JBossSAMLConstants.SUBJECT.get());
    }

}