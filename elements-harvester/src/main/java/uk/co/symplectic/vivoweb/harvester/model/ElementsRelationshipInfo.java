/*
 * ******************************************************************************
 *   Copyright (c) 2019 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.model;

import uk.co.symplectic.utils.xml.XMLEventProcessor;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static uk.co.symplectic.elements.api.ElementsAPI.apiNS;
import static uk.co.symplectic.elements.api.ElementsAPI.atomNS;

/**
 * Subclass of ElementsItemInfo to store and expose a set of data relating to an Elements Relationship.
 * Exposes Type, Visible, Complete, ObjectIds (the set [2] of ItemId's of the linked objects).
 * Also offers getUserIds, and getNonUserIds to get the subset of ObjectIds that either do, or do not represent users.
 */

public class ElementsRelationshipInfo extends ElementsItemInfo{

    /**
     * An XMLEventProcessor.ItemExtractingFilter based Extractor that can be used to extract an ElementsRelationshipInfo
     * object from an XML data stream.
     * Note, can handle processing "deleted" streams, even though the extracted object will typically be almost empty.
     */
    public static class Extractor extends XMLEventProcessor.ItemExtractingFilter<ElementsItemInfo>{

        private static DocumentLocation fileEntryLocation = new DocumentLocation(new QName(atomNS, "entry"), new QName(apiNS, "relationship"));
        private static DocumentLocation feedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "relationship"));
        private static DocumentLocation feedDeletedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "deleted-relationship"));

        @SuppressWarnings("WeakerAccess")
        public static Extractor getExtractor(ElementsItemInfo.ExtractionSource source, int maximumExpected){
            switch(source) {
                case FEED : return new Extractor(feedEntryLocation, maximumExpected);
                case DELETED_FEED : return new Extractor(feedDeletedEntryLocation, maximumExpected);
                case FILE : return new Extractor(fileEntryLocation, maximumExpected);
                default : throw new IllegalStateException("invalid extractor source type requested");
            }
        }

        //Where the item currently being extracted is built.
        private ElementsRelationshipInfo workspace = null;

        private Extractor(DocumentLocation location, int maximumAmountExpected){
            super(location, maximumAmountExpected);
        }

        @Override
        protected void initialiseItemExtraction(XMLEventProcessor.WrappedXmlEvent initialEvent) throws XMLStreamException {
            int id = Integer.parseInt(initialEvent.getAttribute("id"));
            workspace = ElementsItemInfo.createRelationshipItem(id);
        }

        @Override
        protected void processEvent(XMLEventProcessor.WrappedXmlEvent event, List<QName> relativeLocation) throws XMLStreamException {
            if (event.isRelevantForExtraction()) {
                QName name = event.getName();
                //only pull type id for "relationship" not "deleted-relationship" where it is not present.
                if(name.equals(new QName(apiNS, "relationship"))){
                    workspace.setType(event.getAttribute("type"));
                }
                else if (name.equals(new QName(apiNS, "object"))) {
                    try {
                        ElementsObjectCategory objectCategory = ElementsObjectCategory.valueOf(event.getAttribute("category"));
                        int objectID = Integer.parseInt(event.getAttribute("id"));
                        workspace.addObjectId(ElementsItemId.createObjectId(objectCategory, objectID));
                    }
                    catch(IndexOutOfBoundsException e){
                        //do nothing - this is just a relationship to an object type we don't know how to handle yet..
                        //will result in an "incomplete" relationship
                    }
                }
                else if(name.equals(new QName(apiNS, "is-visible"))){
                    //needs to have a value... true or false.. so no has check
                    workspace.setIsVisible(Boolean.parseBoolean(event.getRequiredValue()));
                }
            }
        }

        @Override
        protected ElementsRelationshipInfo finaliseItemExtraction(XMLEventProcessor.WrappedXmlEvent finalEvent){
            return workspace;
        }
    }

    //default visible to "true" so that relationships that are not marked as visible at all (e.g those between a grant and a publication) are definitely included.
    private boolean isVisible = true;
    private String type = null;
    private final List<ElementsItemId.ObjectId> objectIds = new ArrayList<ElementsItemId.ObjectId>();

    //package private as should only ever be constructed by create calls into superclass
    ElementsRelationshipInfo(int id) { super(ElementsItemId.createRelationshipId(id)); }

    public String getType() {
        if(type == null) throw new IllegalAccessError("typeId has not been initialised");
        return type;
    }
    //Note: setters are private as only extractor should use them..
    private void setType(String type) {
        this.type = type;
    }

    public boolean getIsVisible() {
        return isVisible;
    }

    public boolean getIsComplete() {
        return objectIds.size() == 2;
    }

    private void setIsVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public List<ElementsItemId.ObjectId> getUserIds() {
        List<ElementsItemId.ObjectId> userIds = new ArrayList<ElementsItemId.ObjectId>();
        for(ElementsItemId.ObjectId id : objectIds){
            if(id.getItemSubType() == ElementsObjectCategory.USER) userIds.add(id);
        }
        return userIds;
    }

    public List<ElementsItemId.ObjectId> getNonUserIds() {
        List<ElementsItemId.ObjectId> nonUserIds = new ArrayList<ElementsItemId.ObjectId>();
        for(ElementsItemId.ObjectId id : objectIds){
            if(id.getItemSubType() != ElementsObjectCategory.USER) nonUserIds.add(id);
        }
        return nonUserIds;
    }

    private void addObjectId(ElementsItemId.ObjectId id) { objectIds.add(id); }

    public List<ElementsItemId.ObjectId> getObjectIds() { return Collections.unmodifiableList(objectIds); }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder(this.getItemId().toString());
        builder.append(" || ");
        boolean first = true;
        for(ElementsItemId.ObjectId id : getObjectIds()) {
            if(!first) builder.append(" | ");
            builder.append(id.toString());
            first = false;
        }
        return builder.toString();
    }
}
