/*
 * Copyright 2013 Square Inc.
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

package flow;

import android.view.View;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a class that designates a screen. A screen is a distinct part of an application
 * containing all information that describes this state. Optionally specifies its view or layout.
 *
 * <p>For example, <pre><code>
 * {@literal@}Screen(WelcomeScreenView.class)
 * public class WelcomeScreen { ... }
 *
 * {@literal@}Screen(layout=R.layout.conversation_screen_layout)
 * public class ConversationScreen { ... }
 * </code></pre>
 */
@Retention(RUNTIME) @Target(TYPE)
public @interface Screen {
  int layout() default View.NO_ID;
  Class<? extends android.view.View> value() default View.class;
}
