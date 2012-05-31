/*
 * Copyright 2004-2012 the original author or authors.
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

/**
 * Gant script that loads the Griffon interactive shell
 *
 * @author Graeme Rocher (Grails 0.4)
 */

import org.codehaus.groovy.tools.shell.Groovysh
import org.codehaus.groovy.tools.shell.IO

includeTargets << griffonScript('_GriffonBootstrap')

target(name: 'shell', description: "Runs an embedded application in a Groovy Shell", prehook: null, posthook: null) {
    depends(createConfig)

    jardir = ant.antProject.replaceProperties(buildConfig.griffon.jars.destDir)
    ant.copy(todir: jardir) { fileset(dir: "${griffonHome}/lib/", includes: "jline-*.jar") }

    if (!isPluginProject && !isArchetypeProject) {
        bootstrap()
    }

    def b = new Binding()
    if (!isPluginProject && !isArchetypeProject) b.app = griffonApp

    def shell = new Groovysh(classLoader, b, new IO(System.in, System.out, System.err))
    shell.run([] as String[])
}

setDefaultTarget(shell)