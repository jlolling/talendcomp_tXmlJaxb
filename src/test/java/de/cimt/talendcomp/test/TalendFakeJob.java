package de.cimt.talendcomp.test;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import junit.framework.TestCase;
import routines.system.RunStat;

public class TalendFakeJob extends TestCase {

	static Logger log = Logger.getLogger(TalendFakeJob.class);

	public TalendFakeJob() {
		BasicConfigurator.configure();
	}

	protected Map<String, Object> globalMap = new HashMap<String, Object>();
	protected String currentComponent = "";

	protected final String jobVersion = "0.1";
	protected final String jobName = "";
	protected final String projectName = "COMPDEV";
	protected String contextStr = "Default";
	public Integer errorCode = null;

	protected final java.util.Map<String, Long> start_Hash = new java.util.HashMap<String, Long>();
	protected final java.util.Map<String, Long> end_Hash = new java.util.HashMap<String, Long>();
	protected final java.util.Map<String, Boolean> ok_Hash = new java.util.HashMap<String, Boolean>();
	public final java.util.List<String[]> globalBuffer = new java.util.ArrayList<String[]>();

	protected RunStat runStat = new RunStat();

	protected boolean execStat = false;
	protected String iterateId = "";
	protected java.util.Map<String, Object> resourceMap = new java.util.HashMap<String, Object>();
}
