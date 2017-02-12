// GraphTea Project: http://github.com/graphtheorysoftware/GraphTea
// Copyright (C) 2012 Graph Theory Software Foundation: http://GraphTheorySoftware.com
// Copyright (C) 2008 Mathematical Science Department of Sharif University of Technology
// Distributed under the terms of the GNU General Public License (GPL): http://www.gnu.org/licenses/
package graphtea.plugins.main.ui;

import graphtea.graph.old.GShape;
import graphtea.ui.components.gpropertyeditor.editors.inplace.GComboEditor;

/**
 * User: root
 */
public class GShapeEditor extends GComboEditor {
    public Object[] getValues() {
        return new GShape[]{
                GShape.OVAL,
                GShape.RECTANGLE,
                GShape.ROUNDRECT
        };
    }
}
