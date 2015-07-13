package org.codehaus.plexus.compiler.javac;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.ArtifactRequest;

import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Olivier Lamy
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @since 2.0
 */
public class JavaxToolsCompiler
{
    /**
     * is that thread safe ???
     */
    static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();

    private static List<JavaCompiler> JAVA_COMPILERS = new CopyOnWriteArrayList<JavaCompiler>();

    protected static JavaCompiler getJavaCompiler( CompilerConfiguration compilerConfiguration )
    {
        switch ( compilerConfiguration.getCompilerReuseStrategy() )
        {
            case AlwaysNew:
                return ToolProvider.getSystemJavaCompiler();
            case ReuseCreated:
                JavaCompiler javaCompiler;
                synchronized ( JAVA_COMPILERS )
                {
                    if ( JAVA_COMPILERS.size() > 0 )
                    {
                        javaCompiler = JAVA_COMPILERS.get( 0 );
                        JAVA_COMPILERS.remove( javaCompiler );
                        return javaCompiler;
                    }
                }
                javaCompiler = ToolProvider.getSystemJavaCompiler();
                return javaCompiler;
            case ReuseSame:
            default:
                return COMPILER;
        }

    }

    static void releaseJavaCompiler( JavaCompiler javaCompiler, CompilerConfiguration compilerConfiguration )
    {
        if ( javaCompiler == null )
        {
            return;
        }
        if ( compilerConfiguration.getCompilerReuseStrategy()
            == CompilerConfiguration.CompilerReuseStrategy.ReuseCreated )
        {
            JAVA_COMPILERS.add( javaCompiler );
        }
    }

    static CompilerResult compileInProcess( String[] args, final CompilerConfiguration config, String[] sourceFiles )
        throws CompilerException
    {
        JavaCompiler compiler = getJavaCompiler( config );
        try
        {
            if ( compiler == null )
            {
                CompilerMessage message = new CompilerMessage( "No compiler is provided in this environment. "
                                                                   + "Perhaps you are running on a JRE rather than a JDK?",
                                                               CompilerMessage.Kind.ERROR );
                return new CompilerResult( false, Collections.singletonList( message ) );
            }
            final String sourceEncoding = config.getSourceEncoding();
            final Charset sourceCharset = sourceEncoding == null ? null : Charset.forName( sourceEncoding );
            final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();
            final StandardJavaFileManager standardFileManager =
                compiler.getStandardFileManager( collector, null, sourceCharset );

            final Iterable<? extends JavaFileObject> fileObjects =
                standardFileManager.getJavaFileObjectsFromStrings( Arrays.asList( sourceFiles ) );

             /*(Writer out,
             JavaFileManager fileManager,
             DiagnosticListener<? super JavaFileObject> diagnosticListener,
             Iterable<String> options,
             Iterable<String> classes,
             Iterable<? extends JavaFileObject> compilationUnits)*/

            List<String> arguments = Arrays.asList( args );

            final JavaCompiler.CompilationTask task =
                compiler.getTask( null, standardFileManager, collector, arguments, null, fileObjects );

            List<org.codehaus.plexus.compiler.Processor> configProcessors = config.getProcessors();

            if ( configProcessors != null )
            {
                List<Processor> processors = new ArrayList<Processor>();

                for ( org.codehaus.plexus.compiler.Processor configProcessor : configProcessors )
                {
                    final Map<String, String> processorOptions = configProcessor.getOptions();

                    final List<ArtifactRepository> pluginArtifactRepositories = config.getProject().getPluginArtifactRepositories();
                    RepositorySystem repositorySystem = ;
                    repositorySystem.resolveArtifact( MavenRepositorySystemUtils.newSession(), new ArtifactRequest(  ) )

                    // TODO: use processor artifact to acquire a new class loader, if possible; else skip to the next
                    final ClassLoader classLoader = null;
                    final ServiceLoader<Processor> serviceLoader = ServiceLoader.load( Processor.class, classLoader );
                    // all iterator operations have to be in a try block to handle ServiceConfigurationError
                    try {
                        for ( Processor processor : serviceLoader )
                        {
                            if (processorOptions != null) {
                                final Processor delegate = processor;
                                processor = new Processor() {
                                    public Set<String> getSupportedOptions()
                                    {
                                        return delegate.getSupportedOptions();
                                    }

                                    public Set<String> getSupportedAnnotationTypes()
                                    {
                                        return delegate.getSupportedAnnotationTypes();
                                    }

                                    public SourceVersion getSupportedSourceVersion()
                                    {
                                        return delegate.getSupportedSourceVersion();
                                    }

                                    public void init( final ProcessingEnvironment processingEnv )
                                    {
                                        delegate.init( new ProcessingEnvironment() {
                                            public Map<String, String> getOptions()
                                            {
                                                return processorOptions;
                                            }

                                            public Messager getMessager()
                                            {
                                                return processingEnv.getMessager();
                                            }

                                            public Filer getFiler()
                                            {
                                                return processingEnv.getFiler();
                                            }

                                            public Elements getElementUtils()
                                            {
                                                return processingEnv.getElementUtils();
                                            }

                                            public Types getTypeUtils()
                                            {
                                                return processingEnv.getTypeUtils();
                                            }

                                            public SourceVersion getSourceVersion()
                                            {
                                                return processingEnv.getSourceVersion();
                                            }

                                            public Locale getLocale()
                                            {
                                                return processingEnv.getLocale();
                                            }
                                        } );
                                    }

                                    public boolean process( final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv )
                                    {
                                        return delegate.process( annotations, roundEnv );
                                    }

                                    public Iterable<? extends Completion> getCompletions( final Element element, final AnnotationMirror annotation, final ExecutableElement member, final String userText )
                                    {
                                        return delegate.getCompletions( element, annotation, member, userText );
                                    }
                                };
                            }
                            processors.add(processor);
                        }
                    }
                    catch ( ServiceConfigurationError e )
                    {
                        throw new CompilerException( "Failed to load an annotation processor from " + classLoader, e );
                    }
                }

                // override any previously configured -processors or discovery
                task.setProcessors( processors );
            }

            final Boolean result = task.call();
            final ArrayList<CompilerMessage> compilerMsgs = new ArrayList<CompilerMessage>();
            for ( Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics() )
            {
                CompilerMessage.Kind kind = convertKind(diagnostic);
                String baseMessage = diagnostic.getMessage( null );
                if ( baseMessage == null )
                {
                    continue;
                }
                JavaFileObject source = diagnostic.getSource();
                String longFileName = source == null ? null : source.toUri().getPath();
                String shortFileName = source == null ? null : source.getName();
                String formattedMessage = baseMessage;
                int lineNumber = Math.max( 0, (int) diagnostic.getLineNumber() );
                int columnNumber = Math.max( 0, (int) diagnostic.getColumnNumber() );
                if ( source != null && lineNumber > 0 )
                {
                    // Some compilers like to copy the file name into the message, which makes it appear twice.
                    String possibleTrimming = longFileName + ":" + lineNumber + ": ";
                    if ( formattedMessage.startsWith( possibleTrimming ) )
                    {
                        formattedMessage = formattedMessage.substring( possibleTrimming.length() );
                    }
                    else
                    {
                        possibleTrimming = shortFileName + ":" + lineNumber + ": ";
                        if ( formattedMessage.startsWith( possibleTrimming ) )
                        {
                            formattedMessage = formattedMessage.substring( possibleTrimming.length() );
                        }
                    }
                }
                compilerMsgs.add(
                    new CompilerMessage( longFileName, kind, lineNumber, columnNumber, lineNumber, columnNumber,
                                         formattedMessage ) );
            }
            if ( result != Boolean.TRUE && compilerMsgs.isEmpty() )
            {
                compilerMsgs.add(
                    new CompilerMessage( "An unknown compilation problem occurred", CompilerMessage.Kind.ERROR ) );
            }

            return new CompilerResult( result, compilerMsgs );
        }
        catch ( Exception e )
        {
            throw new CompilerException( e.getMessage(), e );
        }
        finally
        {
            releaseJavaCompiler( compiler, config );

        }
    }

    public static CompilerMessage.Kind convertKind(Diagnostic<? extends JavaFileObject> diagnostic) {
        CompilerMessage.Kind kind;
        switch ( diagnostic.getKind() )
        {
            case ERROR:
                kind = CompilerMessage.Kind.ERROR;
                break;
            case WARNING:
                kind = CompilerMessage.Kind.WARNING;
                break;
            case MANDATORY_WARNING:
                kind = CompilerMessage.Kind.MANDATORY_WARNING;
                break;
            case NOTE:
                kind = CompilerMessage.Kind.NOTE;
                break;
            default:
                kind = CompilerMessage.Kind.OTHER;
                break;
        }
        return kind;
    }
}
