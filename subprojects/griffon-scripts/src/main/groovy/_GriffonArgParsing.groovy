/*
* Copyright 2004-2011 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import static org.codehaus.griffon.cli.CommandLineConstants.KEY_INTERACTIVE_MODE

/**
 * Gant script containing the Griffon build event system.
 *
 * @author Peter Ledbrook (Grails 1.1)
 */
// No point doing this stuff more than once.
if (getBinding().variables.containsKey("_args_parsing_called")) return
_args_parsing_called = true

includeTargets << griffonScript("_GriffonSettings")

argsMap = [params: []]

target(name: 'parseArguments', description: "Parse the arguments passed on the command line", prehook: null, posthook: null) {
    // Only ever parse the arguments once. We also don't bother parsing
    // the arguments if the "args" string is empty.
    if (argsMap.size() > 1 || argsMap["params"] || !args) return

    args?.tokenize().each {token ->
        def nameValueSwitch = token =~ "--?(.*?)=(.*)"
        if (nameValueSwitch.matches()) { // this token is a name/value pair (ex: --foo=bar or -z=qux)
            argsMap[nameValueSwitch[0][1]] = nameValueSwitch[0][2]
        }
        else {
            def nameOnlySwitch = token =~ "--?(.*)"
            if (nameOnlySwitch.matches()) {  // this token is just a switch (ex: -force or --help)
                argsMap[nameOnlySwitch[0][1]] = true
            }
            else { // single item tokens, append in order to an array of params
                argsMap["params"] << token
            }
        }
    }

    if (argsMap.containsKey('non-interactive')) {
        println "Setting non-interactive mode"
        isInteractive = !(argsMap.'non-interactive')
        System.setProperty(KEY_INTERACTIVE_MODE, "${isInteractive}")
    }
}
