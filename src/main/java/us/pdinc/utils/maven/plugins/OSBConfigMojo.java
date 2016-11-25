package us.pdinc.utils.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

@Mojo( name = "osbconfig", defaultPhase = LifecyclePhase.COMPILE)
public class OSBConfigMojo extends AbstractMojo
{
	public static final String EXTENSION=".sbconfig.jar";
	@Component
	private MavenProject mavenProject;

	@Component
	private MavenSession mavenSession;

	@Component
	private MojoExecution mojoExecution;
	
	@Component
	private BuildPluginManager pluginManager;
	
	@Parameter(property = "project.build.directory", readonly = true)
	private String outputDirectory;

	@Parameter(property = "project.build.finalName", readonly = true)
	private String finalName;

	private static final String PROPERTY_MW_HOME="osbconfig.mwhome";
	private static final String DEFAULT_MW_HOME="${osbconfig.mwhome}";
	@Parameter(property = PROPERTY_MW_HOME, defaultValue=DEFAULT_MW_HOME, required=true)
	/**
	 * The Oracle Middleware Home (root of the middleware install), as described in http://docs.oracle.com/cd/E25178_01/core.1111/e10064/create.htm#autoId4 .
	 */
	private String mwHome;
	
	private static final String DEFAULT_OSB_HOME="${osbconfig.mwhome}/Oracle_OSB1";
	private static final String PROPERTY_OSB_HOME="osbconfig.osbhome";
	@Parameter(property = PROPERTY_OSB_HOME, defaultValue=DEFAULT_OSB_HOME)
	/**
	 * The ORACLE_HOME for the OSB install. See http://docs.oracle.com/cd/E25178_01/core.1111/e10064/create.htm#autoId6 .
	 */
	private String osbHome;
	
	private static final String DEFAULT_WL_VER="10.3";
	private static final String PROPERTY_WL_VER="osbconfig.wlver";
	@Parameter(property = PROPERTY_WL_VER, defaultValue=DEFAULT_WL_VER)
	/**
	 * The 2 point version of weblogic in use. It has only been tested with 10.3.6, e.g. 10.3.
	 */
	private String wlVer;
	
	private static final String DEFAULT_WL_HOME="${osbconfig.mwhome}/wlserver_${osbconfig.wlver}";
	private static final String PROPERTY_WL_HOME="osbconfig.wlhome";
	@Parameter(property = PROPERTY_WL_HOME, defaultValue=DEFAULT_WL_HOME)
	/**
	 * The WL_HOME as described in http://www.oracle.com/technetwork/middleware/ias/downloads/wls1033-dev-readme-131493.txt .
	 */
	private String wlHome;
	
	private static final String DEFAULT_BEA_HOME="${osbconfig.mwhome}/";
	private static final String PROPERTY_BEA_HOME="osbconfig.beahome";
	@Parameter(property = PROPERTY_BEA_HOME, defaultValue=DEFAULT_BEA_HOME)
	/**
	 * The BEA_HOME, typically the MW_HOME. See https://docs.oracle.com/cd/E13179_01/common/docs103/install/postins.html .
	 */
	private String beaHome;
	
	private static final String DEFAULT_MW_MODULES_DIR="${osbconfig.mwhome}/modules";
	private static final String PROPERTY_MW_MODULES_DIR="osbconfig.mwmodulesdir";
	@Parameter(property = PROPERTY_MW_MODULES_DIR, defaultValue=DEFAULT_MW_MODULES_DIR)
	/**
	 * The middleware's modules dir.
	 */
	private String mwModulesDir;
	
	private static final String DEFAULT_MW_FEATURES_DIR="${osbconfig.mwmodulesdir}/features";
	private static final String PROPERTY_MW_FEATURES_DIR="osbconfig.mwfeaturesdir";
	@Parameter(property = PROPERTY_MW_FEATURES_DIR, defaultValue=DEFAULT_MW_FEATURES_DIR)
	/**
	 * The FEATURES_HOME as described in http://docs.oracle.com/cd/E17904_01/apirefs.1111/e13952/taskhelp/coherence/ConfigureStartupArgumentsForCoherenceServers.html .
	 */
	private String mwFeaturesDir;
	
	private static final String DEFAULT_CONFIGJAR_DIR="${osbconfig.osbhome}/tools/configjar";
	private static final String PROPERTY_CONFIGJAR_DIR="osbconfig.configjardir";
	@Parameter(property = PROPERTY_CONFIGJAR_DIR, defaultValue=DEFAULT_CONFIGJAR_DIR)
	/**
	 * Typically OSB_HOME/tools/configjar as described in https://docs.oracle.com/cd/E28280_01/dev.1111/e15866/app_export.htm#BABCCCGB .
	 */
	private String configJarDir;
	
	private static final String DEFAULT_JAVA_HOME="${java.home}/";
	private static final String PROPERTY_JAVA_HOME="osbconfig.javahome";
	@Parameter(property = PROPERTY_JAVA_HOME, defaultValue=DEFAULT_JAVA_HOME)
	/**
	 * This is an optional JAVA_HOME to use, it will default to the JAVA_HOME used by Maven
	 */
	private String javaHome;
	
	private static final String DEFAULT_JAVA_EXE="${osbconfig.javahome}/bin/java";
	private static final String PROPERTY_JAVA_EXE="osbconfig.javaexe";
	/**
	 * The explicit executable to use. It defaults to bin/java under the current JAVA_HOME. setting it without any path prefix with use the PATH environment variable to find it. 
	 */
	@Parameter(property = PROPERTY_JAVA_EXE, defaultValue=DEFAULT_JAVA_EXE)
	private String javaExecutable;
	
	private static final String DEFAULT_OSBCONFIG_WORK_ROOT="${project.build.directory}/osbconfig-workdir";
	private static final String PROPERTY_OSBCONFIG_WORK_ROOT="osbconfig.workroot";
	@Parameter(property = PROPERTY_OSBCONFIG_WORK_ROOT, defaultValue=DEFAULT_OSBCONFIG_WORK_ROOT)
	private String workRoot;
	
	private static final String DEFAULT_OSBCONFIG_TMP_DIR="${osbconfig.workroot}/tmp";
	private static final String PROPERTY_OSBCONFIG_TMP_DIR="osbconfig.tmpdir";
	@Parameter(property = PROPERTY_OSBCONFIG_TMP_DIR, defaultValue=DEFAULT_OSBCONFIG_TMP_DIR)
	private String tmpDir;
	
	private static final String DEFAULT_OSBCONFIG_WORK_DIR="${osbconfig.workroot}/run";
	private static final String PROPERTY_OSBCONFIG_WORK_DIR="osbconfig.workdir";
	@Parameter(property = PROPERTY_OSBCONFIG_WORK_DIR, defaultValue=DEFAULT_OSBCONFIG_WORK_DIR)
	private String workDir;
	
	private static final String DEFAULT_OSBCONFIG_SETTINGS_FILE="${osbconfig.workroot}/etc/settings.xml";
	private static final String PROPERTY_OSBCONFIG_SETTINGS_FILE="osbconfig.settingsfile";
	@Parameter(property = PROPERTY_OSBCONFIG_SETTINGS_FILE, defaultValue=DEFAULT_OSBCONFIG_SETTINGS_FILE)
	private String settingsFile;
	
	protected void postProcess() throws MojoExecutionException
	{
		try
		{
			Properties props = mavenProject.getProperties();
			if (props == null)
			{
				getLog().warn("Null properties?");
				return;
			}
			
			Log log = getLog();
	
			PluginParameterExpressionEvaluator ppee = new PluginParameterExpressionEvaluator( mavenSession, mojoExecution );
	
			PostProcessor pp=new PostProcessor();
			pp.setPluginParameterExpressionEvaluator(ppee);
			pp.setLog(log);
			pp.setProperties(props);
	
			//no dependencies for MW_HOME
			if (!props.containsKey(PROPERTY_MW_HOME)) props.setProperty(PROPERTY_MW_HOME, mwHome);
			osbHome=		pp.postProcessString(osbHome, 			PROPERTY_OSB_HOME, 					DEFAULT_OSB_HOME);
			wlVer=			pp.postProcessString(wlVer, 			PROPERTY_WL_VER, 					DEFAULT_WL_VER);
			wlHome=			pp.postProcessString(wlHome, 			PROPERTY_WL_HOME, 					DEFAULT_WL_HOME);
			beaHome=		pp.postProcessString(beaHome, 			PROPERTY_BEA_HOME, 					DEFAULT_BEA_HOME);
			mwModulesDir=	pp.postProcessString(mwModulesDir, 		PROPERTY_MW_MODULES_DIR, 			DEFAULT_MW_MODULES_DIR);
			mwFeaturesDir=	pp.postProcessString(mwFeaturesDir, 	PROPERTY_MW_FEATURES_DIR, 			DEFAULT_MW_FEATURES_DIR);
			configJarDir=	pp.postProcessString(configJarDir, 		PROPERTY_CONFIGJAR_DIR, 			DEFAULT_CONFIGJAR_DIR);
			javaHome=		pp.postProcessString(javaHome, 			PROPERTY_JAVA_HOME, 				DEFAULT_JAVA_HOME);
			javaExecutable=	pp.postProcessString(javaExecutable, 	PROPERTY_JAVA_EXE, 					DEFAULT_JAVA_EXE);
			workRoot=		pp.postProcessString(workRoot, 			PROPERTY_OSBCONFIG_WORK_ROOT, 		DEFAULT_OSBCONFIG_WORK_ROOT);
			tmpDir=			pp.postProcessString(tmpDir, 			PROPERTY_OSBCONFIG_TMP_DIR, 		DEFAULT_OSBCONFIG_TMP_DIR);
			workDir=		pp.postProcessString(workDir, 			PROPERTY_OSBCONFIG_WORK_DIR, 		DEFAULT_OSBCONFIG_WORK_DIR);
			settingsFile=	pp.postProcessString(settingsFile, 		PROPERTY_OSBCONFIG_SETTINGS_FILE, 	DEFAULT_OSBCONFIG_SETTINGS_FILE);
		}
		catch (RuntimeException e)
		{
			throw new MojoExecutionException("failed to post process",e);
		}
	}
	
	public static class PostProcessor
	{
		private PluginParameterExpressionEvaluator ppee;
		private Log log;
		private Properties props;
		
 		public String postProcessString(String curVal, String propertyName, String defaultValue)
		{
 			log.debug("about to post process  "+propertyName+"'s value:"+curVal);
			if (defaultValue==null) throw new NullPointerException("defaultValue is null");
			if (propertyName==null) throw new NullPointerException("propertyName is null");
			
			if (curVal==null) 
			{
				log.warn("curVal is null for "+propertyName+", setting to "+defaultValue);
				curVal=defaultValue;
			}
			
			if (defaultValue.equals(curVal))
			{
				try
				{
					curVal=(String) ppee.evaluate(curVal);
					if (curVal==null) throw new NullPointerException("evaluation resulted in null");
				}
				catch (ExpressionEvaluationException|ClassCastException e)
				{
					log.warn("unable to set "+propertyName+", failed to evaluate: "+curVal, e);
				}			
			}
			if (!props.containsKey(propertyName)) props.setProperty(propertyName, curVal);

 			log.debug("done with post process "+propertyName+"'s value:"+curVal);
			return curVal;		
		}

		public void setProperties(Properties props)
		{
			this.props=props;
		}

		public void setLog(Log log)
		{
			this.log=log;
		}

		public void setPluginParameterExpressionEvaluator(PluginParameterExpressionEvaluator ppee)
		{
			this.ppee=ppee;
		}
	}
	
	public void execute() throws MojoExecutionException
	{
		Log log = getLog();
		postProcess();
		
		File outdir = new File(this.outputDirectory + "/" );
		File artifact = new File(outdir, this.finalName + EXTENSION);
		log.debug("artifact:"+artifact);
		
		artifact.getParentFile().mkdirs();
		
		File workDir=new File(this.workRoot);		
		File tmpDir=new File(this.tmpDir);
		tmpDir.mkdirs();
		if (!tmpDir.exists()) throw new IllegalStateException("tmp dir: "+tmpDir+" does not exist");
		
		File runDir=new File(this.workDir);
		runDir.mkdirs();
		if (!runDir.exists()) throw new IllegalStateException("run dir: "+runDir+" does not exist");
		
//		File etcDir=new File(workDir,"etc");
//		etcDir.mkdirs();
//		if (!etcDir.exists()) throw new IllegalStateException("etc dir: "+etcDir+" does not exist");
		
		File settingsFile=new File(this.settingsFile);
		if (!settingsFile.exists()) throw new IllegalStateException("file: "+settingsFile+" does not exist");
		
		CommandLine command=new CommandLine(javaExecutable);
		
		if (!new File(wlHome).exists()) log.warn("${"+PROPERTY_WL_HOME+"}="+wlHome+", which does not exist");
		command.addArgument("-Dweblogic.home="+wlHome,false);
		
		if (!new File(osbHome).exists()) log.warn("${"+PROPERTY_OSB_HOME+"}="+osbHome+", which does not exist");
		command.addArgument("-Dosb.home="+osbHome,false);
		
		command.addArgument("-cp",false);
		
		String classpath=buildClasspath();
		command.addArgument(classpath);
		
		command.addArgument("com.bea.alsb.tools.configjar.ConfigJar");

		command.addArgument("-settingsfile");
		
		command.addArgument(settingsFile.toString());
		
		log.debug("about to execute: "+command);
		
		Map<String, String> env=new HashMap<String,String>();
//		env.put("temp", tmpDir.toString());
//		env.put("tmp", tmpDir.toString());
		env.put("TEMP", tmpDir.toString());
		env.put("TMP", tmpDir.toString());
		
		Executor exec = new DefaultExecutor();
		exec.setWorkingDirectory(runDir);

		try
		{
			exec.execute(command,env);
		}
		catch (IOException e1)
		{
			throw new MojoExecutionException("failed to run configjar",e1);
		}
		
		this.mavenProject.getArtifact().setFile(artifact);
	}

	private String buildClasspath()
	{	
		StringBuilder sb=new StringBuilder();
		Log log = getLog();
		File fmwhome=new File(mwHome);
		File fwlhome=new File(wlHome);
		File fosbhome=new File(osbHome);
		File fconfigjardir=new File(configJarDir);
		
		File[] entries={
				new File(fmwhome,"modules/features/weblogic.server.modules_10.3.6.0.jar"),
				new File(fwlhome,"server/lib/weblogic.jar"),
				new File(fmwhome,"oracle_common/modules/oracle.http_client_11.1.1.jar"),
				new File(fmwhome,"oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar"),
				new File(fmwhome,"oracle_common/modules/oracle.webservices_11.1.1/orawsdl.jar"),
				new File(fmwhome,"oracle_common/modules/oracle.wsm.common_11.1.1/wsm-dependencies.jar"),
				new File(fosbhome,"modules/features/osb.server.modules_11.1.1.7.jar"),
				new File(fosbhome,"soa/modules/oracle.soa.common.adapters_11.1.1/oracle.soa.common.adapters.jar"),
				new File(fosbhome,"lib/external/log4j_1.2.8.jar"),
				new File(fosbhome,"lib/alsb.jar"),
				new File(fconfigjardir,"configjar.jar"),
				new File(fconfigjardir,"L10N"),
		};
		boolean emptyClassPath=true;
		for (File entry:entries)
		{
			if (!entry.exists())
			{
				log.warn(entry+" does not exist, not adding to classpath");
			}
			else
			{
				log.debug("adding to classpath: "+entry);
				if (!emptyClassPath)
				{
					sb.append(File.pathSeparatorChar);
				}
				emptyClassPath&=false;
				sb.append(entry);
			}
		}
		
		return emptyClassPath?null:sb.toString();
	}
	
}
