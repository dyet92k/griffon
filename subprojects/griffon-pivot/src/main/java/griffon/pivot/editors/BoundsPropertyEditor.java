/*
 * Copyright 2008-2014 the original author or authors.
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
package griffon.pivot.editors;

import griffon.core.editors.AbstractPropertyEditor;
import griffon.metadata.PropertyEditorFor;
import org.apache.pivot.wtk.Bounds;

import java.util.List;
import java.util.Map;

import static griffon.util.GriffonNameUtils.isBlank;

/**
 * @author Andres Almiray
 * @since 2.0.0
 */
@PropertyEditorFor(Bounds.class)
public class BoundsPropertyEditor extends AbstractPropertyEditor {
    public String getAsText() {
        if (null == getValue()) return null;
        Bounds r = (Bounds) getValue();
        return r.x + ", " + r.y + ", " + r.width + ", " + r.height;
    }

    protected void setValueInternal(Object value) {
        if (null == value) {
            super.setValueInternal(null);
        } else if (value instanceof CharSequence) {
            handleAsString(String.valueOf(value));
        } else if (value instanceof List) {
            handleAsList((List) value);
        } else if (value instanceof Map) {
            handleAsMap((Map) value);
        } else if (value instanceof Bounds) {
            super.setValueInternal(value);
        } else {
            throw illegalValue(value, Bounds.class);
        }
    }

    private void handleAsString(String str) {
        if (isBlank(str)) {
            super.setValueInternal(null);
            return;
        }

        String[] parts = str.split(",");
        switch (parts.length) {
            case 4:
                int x = parseValue(parts[0]);
                int y = parseValue(parts[1]);
                int w = parseValue(parts[2]);
                int h = parseValue(parts[3]);
                super.setValueInternal(new Bounds(x, y, w, h));
                break;
            default:
                throw illegalValue(str, Bounds.class);
        }
    }

    private void handleAsList(List<?> list) {
        if(list.isEmpty()) {
            super.setValueInternal(null);
            return;
        }

        switch (list.size()) {
            case 4:
                int x = parseValue(list.get(0));
                int y = parseValue(list.get(1));
                int w = parseValue(list.get(2));
                int h = parseValue(list.get(3));
                super.setValueInternal(new Bounds(x, y, w, h));
                break;
            default:
                throw illegalValue(list, Bounds.class);
        }
    }

    private void handleAsMap(Map<?, ?> map) {
        if(map.isEmpty()) {
            super.setValueInternal(null);
            return;
        }

        int x = getMapValue(map, "x", 0);
        int y = getMapValue(map, "y", 0);
        int w = getMapValue(map, "width", 0);
        int h = getMapValue(map, "height", 0);
        super.setValueInternal(new Bounds(x, y, w, h));
    }

    private int parseValue(Object value) {
        if (value instanceof CharSequence) {
            return parse(String.valueOf(value));
        } else if (value instanceof Number) {
            return parse((Number) value);
        }
        throw illegalValue(value, Bounds.class);
    }

    private int parse(String val) {
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            throw illegalValue(val, Bounds.class, e);
        }
    }

    private int parse(Number val) {
        return val.intValue();
    }

    private int getMapValue(Map<?, ?> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (null == val) val = map.get(String.valueOf(key.charAt(0)));
        if (null == val) {
            return defaultValue;
        } else if (val instanceof CharSequence) {
            return parse(String.valueOf(val));
        } else if (val instanceof Number) {
            return parse((Number) val);
        }
        throw illegalValue(map, Bounds.class);
    }
}
