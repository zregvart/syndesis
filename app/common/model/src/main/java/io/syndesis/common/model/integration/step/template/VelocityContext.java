/*
 * Copyright (C) 2016 Red Hat, Inc.
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
package io.syndesis.common.model.integration.step.template;

import java.util.ArrayList;
import java.util.List;

class VelocityContext extends ProcessingContext {
    // Record velocity-only symbols to ignore them since they don't require a prefix
    final List<String> vOnlySymbols = new ArrayList<>();

    // Flag if the symbol is a declaration for a velocity-only symbol
    boolean vSymbolDeclaration;
}
