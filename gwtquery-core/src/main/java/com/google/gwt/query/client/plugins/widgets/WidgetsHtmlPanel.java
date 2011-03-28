/*
 * Copyright 2011, The gwtquery team.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.query.client.plugins.widgets;

import static com.google.gwt.query.client.GQuery.$;

import com.google.gwt.dom.client.Element;
import com.google.gwt.query.client.GQuery;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.GqUi;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This {@link HTMLPanel} takes as content the outer html of an element already
 * attached to the DOM. If {@link Widget widgets} are present in the children of
 * the element to attach, they will be automatically adopted by the panel in
 * order to keep a consistent widgets hierarchy.
 * 
 */
public class WidgetsHtmlPanel extends HTMLPanel {

  public WidgetsHtmlPanel(Element e) {
    super("");
    e.removeFromParent();
    getElement().appendChild(e);
    adoptSubWidgets();
  }

  /**
   * Iterate over the children of this widget's element to find widget. If
   * widgets are find adopt it automatically
   */
  protected void adoptSubWidgets() {
    adoptSubWidgets(getElement());
  }

  /**
   * Check if the {@link Element Element} <code>root</code> is attached to the
   * widget. If it is the case, adopt the widget. If not, check if the chidren
   * are linked to a widget to adopt them.
   * 
   */
  protected void adoptSubWidgets(Element root) {

    GQuery children = $(root).children();

    for (Element child : children.elements()) {
      Widget w = $(child).widget();
      if (w != null) {
        doAdopt(w);
      } else {
        adoptSubWidgets(child);
      }
    }
  }

  /**
   * Adopt the {@link Widget widet} <code>w</code>. if the current parent of the
   * widget is an {@link HTMLPanel} or is null, the widget will not detach
   * physically in order to maintain the html structure. If the parent is an
   * other widget, it will be physically detach and reattach to this panel.
   * 
   * @param w
   */
  protected void doAdopt(Widget w) {
    Widget parent = w.getParent();
    boolean mustBePhysicallyReattach = false;

    if (parent == null) {
      if (RootPanel.isInDetachList(w)) {
        RootPanel.detachNow(w);
      }

    } else if (parent instanceof HTMLPanel) {
      GqUi.doLogicalDetachFromHtmlPanel(w);

    } else {
      // the widget will be physically detach
      w.removeFromParent();
      mustBePhysicallyReattach = true;
    }

    getChildren().add(w);

    if (mustBePhysicallyReattach) {
      DOM.appendChild(getElement(), w.getElement());
    }

    adopt(w);

  }
}