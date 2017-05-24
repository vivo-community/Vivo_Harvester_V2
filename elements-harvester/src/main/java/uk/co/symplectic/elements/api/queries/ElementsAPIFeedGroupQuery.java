/**
 * Created by ajpc2_000 on 25/07/2016.
 */


package uk.co.symplectic.elements.api.queries;

import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.ElementsFeedQuery;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;

import java.util.Collections;
import java.util.Set;

public class ElementsAPIFeedGroupQuery extends ElementsFeedQuery {
    public ElementsAPIFeedGroupQuery(){
        //groups resource has no concept of ref/full detail level..
        //groups resource is not paginated..
        super(ElementsItemType.GROUP, false);
    }

    @Override
    protected Set<String> getUrlStrings(String apiBaseUrl, ElementsAPIURLBuilder builder, int perPage) {
        return Collections.singleton(builder.buildGroupQuery(apiBaseUrl, this));
    }
}