package org.sourceheads.open.jhch;

import com.jayway.jsonpath.JsonModel;

/**
 * (...)
 *
 * @author Stefan Fiedler
 */
public class VelocityJsonHelper {

    private final JsonModel jsonModel;

    public VelocityJsonHelper(final JsonModel jsonModel) {
        this.jsonModel = jsonModel;
    }

    public String get(final String path) {
        return jsonModel.get(path);
    }

    public boolean has(final String path) {
        return jsonModel.hasPath(path);
    }

    public String getUrl(final String path) {
        final String url = jsonModel.get(path);
        return url.replaceAll("\\\\", "");
    }
}
