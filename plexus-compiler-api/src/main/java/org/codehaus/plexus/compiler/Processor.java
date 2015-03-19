/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.codehaus.plexus.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;

/**
 * An annotation processor definition.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class Processor
{
    /**
     * The group ID of the processor artifact in the repository.
     */
    private String groupId;

    /**
     * The artifact ID of the processor artifact in the repository.
     */
    private String artifactId;

    /**
     * The version (or valid range of versions) of the processor artifact to be
     * used.
     */
    private String version;

    /**
     * Processor dependencies.
     */
    private List<Dependency> dependencies;

    /**
     * Customized processor options.
     */
    private Map<String, String> options;

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( final String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( final String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( final String version )
    {
        this.version = version;
    }

    public List<Dependency> getDependencies()
    {
        if ( this.dependencies == null )
        {
            this.dependencies = new ArrayList<Dependency>();
        }

        return this.dependencies;
    }

    public Map<String, String> getOptions()
    {
        return options;
    }

    public void setOptions( final Map<String, String> options )
    {
        this.options = options;
    }
}
