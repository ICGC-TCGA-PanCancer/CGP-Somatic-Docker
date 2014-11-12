package io.seqware.pancancer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import net.sourceforge.seqware.pipeline.workflowV2.AbstractWorkflowDataModel;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;
import org.apache.commons.lang.StringUtils;

/**
 * <p>For more information on developing workflows, see the documentation at
 * <a href="http://seqware.github.io/docs/6-pipeline/java-workflows/">SeqWare Java Workflows</a>.</p>
 *
 * Quick reference for the order of methods called:
 * 1. setupDirectory
 * 2. setupFiles
 * 3. setupWorkflow
 * 4. setupEnvironment
 * 5. buildWorkflow
 *
 * See the SeqWare API for
 * <a href="http://seqware.github.io/javadoc/stable/apidocs/net/sourceforge/seqware/pipeline/workflowV2/AbstractWorkflowDataModel.html#setupDirectory%28%29">AbstractWorkflowDataModel</a>
 * for more information.
 * 
 * TODO:
 * - Keiran, I think you want to review what happens when test mode and upload-test mode are active.
 * 
 */
public class CgpCnIndelSnvStrWorkflow extends AbstractWorkflowDataModel {

  private static String OUTDIR = "outdir";
  private static String TIMEDIR;
  private boolean testMode=false;
  private boolean cleanup = false;
  
  // datetime all upload files will be named with
  DateFormat df = new SimpleDateFormat("yyyyMMdd");
  String dateString = df.format(Calendar.getInstance().getTime());

  private String workflowName = "svcp_1-0-0";
  
  // MEMORY variables //
  private String  memBasFileGet, memGnosDownload, memPackageResults, memMarkTime,
                  memQcMetrics, memUpload, memGetTbi,
                  // ascat memory
                  memAlleleCount, memAscat, memAscatFinalise,
                  // pindel memory
                  memPindelInput, memPindel, memPindelVcf, memPindelMerge , memPindelFlag,
                  // brass memory
                  memBrassInput, memBrassGroup, memBrassFilter, memBrassSplit,
                  memBrassAssemble, memBrassGrass, memBrassTabix,
                  // caveman memory
                  memCaveCnPrep,
                  memCavemanSetup, memCavemanSplit, memCavemanSplitConcat,
                  memCavemanMstep, memCavemanMerge, memCavemanEstep,
                  memCavemanMergeResults, memCavemanAddIds, memCavemanFlag,
                  memCavemanTbiClean
          ;

  // workflow variables
  private String  // reference variables
                  species, assembly,
                  // sequencing type/protocol
                  seqType, seqProtocol,
                  //GNOS identifiers
                  pemFile, gnosServer, uploadServer,
                  // ascat variables
                  gender,
                  // pindel variables
                  refExclude, pindelGermline,
                  //caveman variables
                  tabixSrvUri,
                  //general variables
                  installBase, refBase, genomeFaGz;
  
  private int coresAddressable;
  
  private void init() {
    try {
      //optional properties
      String outDir = OUTDIR;
      String outPrefix = "";
      if (hasPropertyAndNotNull("output_dir")) {
        outDir = getProperty("output_dir");
      }
      if (hasPropertyAndNotNull("output_prefix")) {
        outPrefix = getProperty("output_prefix");
      }
      if (!"".equals(outPrefix)) {
        if (outPrefix.endsWith("/")) {
          OUTDIR = outPrefix+outDir;
        } else {
          OUTDIR = outPrefix + "/" + outDir;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    TIMEDIR = OUTDIR + "/timings";
  }

  @Override
  public void setupDirectory() {
    //since setupDirectory is the first method run, we use it to initialize variables too.
    init();
    // creates a dir1 directory in the current working directory where the workflow runs
    addDirectory(OUTDIR);
    addDirectory(TIMEDIR);
  }

  @Override
  public Map<String, SqwFile> setupFiles() {
    try {
      
      if(hasPropertyAndNotNull("cleanup")) {
        cleanup = Boolean.valueOf(getProperty("cleanup"));
      }
      
      if(hasPropertyAndNotNull("testMode")) {
        testMode=Boolean.valueOf(getProperty("testMode"));
        System.err.println("WARNING\n\tRunning in test mode, direct access BAM files will be used, change 'testMode' in ini file to disable\n");
      }
      
      if(hasPropertyAndNotNull("uploadServer")) {
        uploadServer = getProperty("uploadServer");
      }
      else {
        System.err.println("WARNING\n\t'uploadServer' not defined in workflow.ini, no VCF upload will be attempted\n");
      }
      
      // used by steps that can use all available cores
      coresAddressable = Integer.valueOf(getProperty("coresAddressable"));

      // MEMORY //
      memBasFileGet = getProperty("memBasFileGet");
      memGnosDownload = getProperty("memGnosDownload");
      memPackageResults = getProperty("memPackageResults");
      memMarkTime = getProperty("memMarkTime");
      memQcMetrics = getProperty("memQcMetrics");
      memUpload = getProperty("memUpload");
      memGetTbi = getProperty("memGetTbi");
      
      memAlleleCount = getProperty("memAlleleCount");
      memAscat = getProperty("memAscat");
      memAscatFinalise = getProperty("memAscatFinalise");
      
      memPindelInput = getProperty("memPindelInput");
      memPindel = getProperty("memPindel");
      memPindelVcf = getProperty("memPindelVcf");
      memPindelMerge = getProperty("memPindelMerge");
      memPindelFlag = getProperty("memPindelFlag");
      
      memBrassInput = getProperty("memBrassInput");
      memBrassGroup = getProperty("memBrassGroup");
      memBrassFilter = getProperty("memBrassFilter");
      memBrassSplit = getProperty("memBrassSplit");
      memBrassAssemble = getProperty("memBrassAssemble");
      memBrassGrass = getProperty("memBrassGrass");
      memBrassTabix = getProperty("memBrassTabix");
      
      memCaveCnPrep = getProperty("memCaveCnPrep");
      memCavemanSetup = getProperty("memCavemanSetup");
      memCavemanSplit = getProperty("memCavemanSplit");
      memCavemanSplitConcat = getProperty("memCavemanSplitConcat");
      memCavemanMstep = getProperty("memCavemanMstep");
      memCavemanMerge = getProperty("memCavemanMerge");
      memCavemanEstep = getProperty("memCavemanEstep");
      memCavemanMergeResults = getProperty("memCavemanMergeResults");
      memCavemanAddIds = getProperty("memCavemanAddIds");
      memCavemanFlag = getProperty("memCavemanFlag");
      memCavemanTbiClean = getProperty("memCavemanTbiClean");

      // REFERENCE INFO //
      species = getProperty("species");
      assembly = getProperty("assembly");
      
      // Sequencing info
      seqType = getProperty("seqType");
      if(seqType.equals("WGS")) {
        seqProtocol = "genomic";
      }

      // Specific to ASCAT workflow //
      gender = getProperty("gender");
      
      // Specific to Caveman workflow //
      tabixSrvUri = getProperty("tabixSrvUri");

      // pindel specific
      refExclude = getProperty("refExclude");

      // used for upload too so always get it
      if(hasPropertyAndNotNull("gnosServer")) {
        gnosServer = getProperty("gnosServer");
      }      
      
      // test mode
      if(!testMode || (hasPropertyAndNotNull("upload-test") && Boolean.valueOf(getProperty("upload-test")))) {
        pemFile = getProperty("pemFile");
      }

      //environment
      installBase = getWorkflowBaseDir() + "/bin/opt";
      refBase = getWorkflowBaseDir() + "/cgp_reference";
      genomeFaGz = getWorkflowBaseDir() + "/data/reference/genome.fa.gz";
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    
    System.err.println("NOTE: completed setup\n");
    
    return getFiles();
  }
  
  private Job bamProvision(String analysisId, String bamFile, Job startTiming) {
    Job basJob = null;
    if(testMode == false) {
      Job gnosDownload = gnosDownloadBaseJob(analysisId);
      gnosDownload.setMaxMemory(memGnosDownload);
      gnosDownload.addParent(startTiming);
      // the file needs to end up in tumourBam/controlBam

      // get the BAS files
      basJob = basFileBaseJob(analysisId, bamFile);
      basJob.setMaxMemory(memBasFileGet);
      basJob.addParent(gnosDownload);
    }
    return basJob;
  }
  
  @Override
  public void buildWorkflow() {
    System.err.println("NOTE: starting buildWorkflow\n");
    Job controlBasJob = null;
    String controlBam;
    List<String> tumourBams = new ArrayList<String>();
    List<Job> tumourBasJobs = new ArrayList<Job>();
    
    List<String> tumourAnalysisIds = new ArrayList<String>();
    List<String> tumourAliquotIds = new ArrayList<String>();
    String controlAnalysisId = new String();
    
    Job startWorkflow = markTime("start");
    startWorkflow.setMaxMemory(memMarkTime);
    
    System.err.println("NOTE: startWorkflow\n");
    
    try {
      if(testMode) {
        controlBam = getProperty("controlBamT");
        tumourBams = Arrays.asList(getProperty("tumourBamT").split(":"));
        for(String t : tumourBams) {
          tumourBasJobs.add(null);
        }
        // only do this if test mode but upload is enabled, we're cheating a bit here but need the values defined with something to do the test upload
        controlAnalysisId = getProperty("controlAnalysisId");
        tumourAnalysisIds = Arrays.asList(getProperty("tumourAnalysisIds").split(":"));
        tumourAliquotIds = Arrays.asList(getProperty("tumourAliquotIds").split(":"));
      }
      else {
        controlAnalysisId = getProperty("controlAnalysisId");
        controlBasJob = bamProvision(controlAnalysisId, getProperty("controlBam"), startWorkflow);
        controlBam = controlAnalysisId + "/" + getProperty("controlBam");

        tumourAnalysisIds = Arrays.asList(getProperty("tumourAnalysisIds").split(":"));
        tumourAliquotIds = Arrays.asList(getProperty("tumourAliquotIds").split(":"));
        List<String> rawBams = Arrays.asList(getProperty("tumourBams").split(":"));
        if(rawBams.size() != tumourAnalysisIds.size()) {
          throw new RuntimeException("Properties tumourAnalysisId and tumourBam decode to lists of different sizes");
        }
        if(rawBams.size() != tumourAliquotIds.size()) {
          throw new RuntimeException("Properties tumourAliquotIds and tumourBam decode to lists of different sizes");
        }
        for(int i=0; i<rawBams.size(); i++) {
          Job tumourBasJob = bamProvision(tumourAnalysisIds.get(i), rawBams.get(i), startWorkflow);
          tumourBasJobs.add(tumourBasJob);
          String tumourBam = tumourAnalysisIds.get(i) + "/" + rawBams.get(i);
          tumourBams.add(tumourBam);
        }
      }
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
    
    System.err.println("NOTE: done file setup\n");
    
    Job getTbiJob = stageTbi();
    getTbiJob.setMaxMemory(memGetTbi);
    getTbiJob.addParent(startWorkflow);
    if(controlBasJob != null) {
      getTbiJob.addParent(controlBasJob);
      for(Job job : tumourBasJobs) {
        getTbiJob.addParent(job);
      }
    }
    
    System.err.println("NOTE: getTbiJob done\n");
    
    Job[] cavemanFlagJobs = new Job [tumourBams.size()];
    for(int i=0; i<tumourBams.size(); i++) {
      System.err.println("NOTE: setup pair" + i + "\n");
      Job cavemanFlagJob = buildPairWorkflow(getTbiJob, controlBam, tumourBams.get(i), i);
      cavemanFlagJobs[i] = cavemanFlagJob;
    }
    
    Job cavemanTbiCleanJob = cavemanTbiCleanJob();
    cavemanTbiCleanJob.setMaxMemory(memCavemanTbiClean);
    for(Job cavemanFlagJob : cavemanFlagJobs) {
      cavemanTbiCleanJob.addParent(cavemanFlagJob);
    }
    
    Job endWorkflow = markTime("end");
    endWorkflow.setMaxMemory(memMarkTime);
    endWorkflow.addParent(cavemanTbiCleanJob);
    
    Job metricsJob = getMetricsJob(tumourBams);
    metricsJob.setMaxMemory(memQcMetrics);
    metricsJob.addParent(endWorkflow);
    
    if(uploadServer != null) {
      String[] resultTypes = {"snv_mnv","cnv","sv","indel"};
      Job uploadJob = vcfUpload(resultTypes, controlAnalysisId, tumourAnalysisIds, tumourAliquotIds);
      uploadJob.setMaxMemory(memUpload);
      uploadJob.addParent(metricsJob);
      
      if (cleanup) {
        // if we upload to GNOS then go ahead and delete all the large files
        Job cleanJob = postUploadCleanJob();
        cleanJob.addParent(uploadJob);
      }
      
    } else {
      // delete just the BAM inputs and not the output dir
      if (cleanup) {
        Job cleanInputsJob = cleanInputsJob();
        cleanInputsJob.addParent(metricsJob);
      }
    }
  }

  /**
   * This builds the workflow for a pair of samples
   * The generic buildWorkflow section will choose the pair to be processed and 
   * setup the control sample download
   */
  private Job buildPairWorkflow(Job getTbiJob, String controlBam, String tumourBam, int tumourCount) {
    
    /**
     * ASCAT - Copynumber
     * Depends on
     *  - tumour/control BAMs
     *  - Gender, will attempt to determine if not specified
     */

    Job[] alleleCountJobs = new Job[2];
    for(int i=0; i<2; i++) {
      Job alleleCountJob = cgpAscatBaseJob(tumourCount, tumourBam, controlBam, "ASCAT", "allele_count", i+1);
      alleleCountJob.setMaxMemory(memAlleleCount);
      alleleCountJob.addParent(getTbiJob);
      alleleCountJobs[i] = alleleCountJob;
    }

    Job ascatJob = cgpAscatBaseJob(tumourCount, tumourBam, controlBam, "ASCAT", "ascat", 1);
    ascatJob.setMaxMemory(memAscat);
    ascatJob.addParent(alleleCountJobs[0]);
    ascatJob.addParent(alleleCountJobs[1]);
    
    Job ascatFinaliseJob = cgpAscatBaseJob(tumourCount, tumourBam, controlBam, "ASCAT", "finalise", 1);
    ascatFinaliseJob.setMaxMemory(memAscatFinalise);
    ascatFinaliseJob.addParent(ascatJob);
    
    Job ascatPackage = packageResults(tumourCount, "ascat", "cnv", tumourBam, "copynumber.caveman.vcf.gz", workflowName, "somatic", dateString);
    ascatPackage.setMaxMemory(memPackageResults);
    ascatPackage.addParent(ascatFinaliseJob);
    
    /**
     * CaVEMan setup is here to allow better workflow graph
     * Messy but necessary
     */
    
    Job caveCnPrepJobs[] = new Job[2];
    for(int i=0; i<2; i++) {
      Job caveCnPrepJob;
      if(i==0) {
        caveCnPrepJob = caveCnPrep(tumourCount, "tumour");
      }
      else {
        caveCnPrepJob = caveCnPrep(tumourCount, "normal");
      }
      caveCnPrepJob.addParent(getTbiJob);
      caveCnPrepJob.addParent(ascatFinaliseJob); // ASCAT dependency!!!
      caveCnPrepJobs[i] = caveCnPrepJob;
    }
    
    
    Job cavemanSetupJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "setup", 1);
    cavemanSetupJob.setMaxMemory(memCavemanSetup);
    cavemanSetupJob.addParent(caveCnPrepJobs[0]);
    cavemanSetupJob.addParent(caveCnPrepJobs[1]);
    // some dependencies handled by ascat step
  
    /**
     * Pindel - InDel calling
     * Depends on:
     *  - tumour/control BAMs
     */
     
    Job[] pindelInputJobs = new Job[2];
    for(int i=0; i<2; i++) {
      Job inputParse = pindelBaseJob(tumourCount, tumourBam, controlBam, "cgpPindel", "input", i+1);
      inputParse.setMaxMemory(memPindelInput);
      inputParse.addParent(getTbiJob);
      pindelInputJobs[i] = inputParse;
    }
    
    // determine number of refs to process
    // we know that this is static for PanCancer so be lazy 24 jobs (1-22,X,Y)
    // but pindel needs to know the exclude list so hard code this
    Job pinVcfJobs[] = new Job[24];
    for(int i=0; i<24; i++) {
      Job pindelJob = pindelBaseJob(tumourCount, tumourBam, controlBam, "cgpPindel", "pindel", i+1);
      pindelJob.setMaxMemory(memPindel);
      pindelJob.addParent(pindelInputJobs[0]);
      pindelJob.addParent(pindelInputJobs[1]);
      
      Job pinVcfJob = pindelBaseJob(tumourCount, tumourBam, controlBam, "cgpPindel", "pin2vcf", i+1);
      pinVcfJob.setMaxMemory(memPindelVcf);
      pinVcfJob.addParent(pindelJob);
      
      // pinVcf depends on pindelJob so only need have dependency on the pinVcf
      pinVcfJobs[i] = pinVcfJob;
    }
    
    Job pindelMergeJob = pindelBaseJob(tumourCount, tumourBam, controlBam, "cgpPindel", "merge", 1);
    pindelMergeJob.setMaxMemory(memPindelMerge);
    for (Job parent : pinVcfJobs) {
      pindelMergeJob.addParent(parent);
    }
    
    Job pindelFlagJob = pindelBaseJob(tumourCount, tumourBam, controlBam, "cgpPindel", "flag", 1);
    pindelFlagJob.setMaxMemory(memPindelFlag);
    pindelFlagJob.addParent(pindelMergeJob);
    pindelFlagJob.addParent(cavemanSetupJob);
    
    Job pindelPackage = packageResults(tumourCount, "pindel", "indel", tumourBam, "flagged.vcf.gz", workflowName, "somatic", dateString);
    pindelPackage.setMaxMemory(memPackageResults);
    pindelPackage.addParent(pindelFlagJob);
    
    /**
     * BRASS - BReakpoint AnalySiS
     * Depends on:
     *  - tumour/control BAMs
     *  - ASCAT output at filter step
     */
    
    Job brassInputJobs[] = new Job[2];
    for(int i=0; i<2; i++) {
      Job brassInputJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "input", i+1);
      brassInputJob.setMaxMemory(memBrassInput);
      brassInputJob.addParent(getTbiJob);
      brassInputJobs[i] = brassInputJob;
    }
    
    Job brassGroupJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "group", 1);
    brassGroupJob.setMaxMemory(memBrassGroup);
    brassGroupJob.addParent(brassInputJobs[0]);
    brassGroupJob.addParent(brassInputJobs[1]);
    
    Job brassFilterJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "filter", 1);
    brassFilterJob.setMaxMemory(memBrassFilter);
    brassFilterJob.addParent(brassGroupJob);
    brassFilterJob.addParent(ascatFinaliseJob); // NOTE: dependency on ASCAT!!
    
    Job brassSplitJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "split", 1);
    brassSplitJob.setMaxMemory(memBrassSplit);
    brassSplitJob.addParent(brassFilterJob);
    
    List<Job> brassAssembleJobs = new ArrayList<Job>();
    for(int i=0; i<coresAddressable; i++) {
      Job brassAssembleJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "assemble", i+1);
      brassAssembleJob.setMaxMemory(memBrassAssemble);
      brassAssembleJob.addParent(brassSplitJob);
      brassAssembleJobs.add(brassAssembleJob);
    }
    
    Job brassGrassJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "grass", 1);
    brassGrassJob.setMaxMemory(memBrassGrass);
    for(Job brassAssembleJob : brassAssembleJobs) {
      brassGrassJob.addParent(brassAssembleJob);
    }
    
    Job brassTabixJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "tabix", 1);
    brassTabixJob.setMaxMemory(memBrassTabix);
    brassTabixJob.addParent(brassGrassJob);
    
    Job brassPackage = packageResults(tumourCount, "brass", "sv", tumourBam, "annot.vcf.gz", workflowName, "somatic", dateString);
    brassPackage.setMaxMemory(memPackageResults);
    brassPackage.addParent(brassTabixJob);
    
    
    /**
     * CaVEMan - SNV analysis
     * !! see above as setup done earlier to help with workflow structure !!
     * Depends on:
     *  - tumour/control BAMs (but depend on BAS for better workflow)
     *  - ASCAT from outset
     *  - pindel at flag step
     */
    
    // should really line count the fai file
    Job cavemanSplitJobs[] = new Job[86];
    for(int i=0; i<86; i++) {
      Job cavemanSplitJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "split", i+1);
      cavemanSplitJob.setMaxMemory(memCavemanSplit);
      cavemanSplitJob.addParent(cavemanSetupJob);
      cavemanSplitJobs[i] = cavemanSplitJob;
    }
    
    Job cavemanSplitConcatJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "split_concat", 1);
    cavemanSplitConcatJob.setMaxMemory(memCavemanSplitConcat);
    for (Job cavemanSplitJob : cavemanSplitJobs) {
      cavemanSplitConcatJob.addParent(cavemanSplitJob);
    }
    
    List<Job> cavemanMstepJobs = new ArrayList<Job>();
    for(int i=0; i<coresAddressable; i++) {
      Job cavemanMstepJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "mstep", i+1);
      cavemanMstepJob.setMaxMemory(memCavemanMstep);
      cavemanMstepJob.addParent(cavemanSplitConcatJob);
      cavemanMstepJobs.add(cavemanMstepJob);
    }
    
    Job cavemanMergeJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "merge", 1);
    cavemanMergeJob.setMaxMemory(memCavemanMerge);
    for(Job cavemanMstepJob : cavemanMstepJobs) {
      cavemanMergeJob.addParent(cavemanMstepJob);
    }
    
    List<Job> cavemanEstepJobs = new ArrayList<Job>();
    for(int i=0; i<coresAddressable; i++) {
      Job cavemanEstepJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "estep", i+1);
      cavemanEstepJob.setMaxMemory(memCavemanEstep);
      cavemanEstepJob.addParent(cavemanMergeJob);
      cavemanEstepJobs.add(cavemanEstepJob);
    }
    
    Job cavemanMergeResultsJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "merge_results", 1);
    cavemanMergeResultsJob.setMaxMemory(memCavemanMergeResults);
    for(Job cavemanEstepJob : cavemanEstepJobs) {
      cavemanMergeResultsJob.addParent(cavemanEstepJob);
    }
    
    Job cavemanAddIdsJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "add_ids", 1);
    cavemanAddIdsJob.setMaxMemory(memCavemanAddIds);
    cavemanAddIdsJob.addParent(cavemanMergeResultsJob);
    
    Job cavemanFlagJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "flag", 1);
    cavemanFlagJob.setMaxMemory(memCavemanFlag);
    cavemanFlagJob.addParent(getTbiJob);
    cavemanFlagJob.addParent(pindelFlagJob); // PINDEL dependency
    cavemanFlagJob.addParent(cavemanAddIdsJob);
    
    Job cavemanPackage = packageResults(tumourCount, "caveman", "snv_mnv", tumourBam, "flagged.muts.vcf.gz", workflowName, "somatic", dateString);
    cavemanPackage.setMaxMemory(memPackageResults);
    cavemanPackage.addParent(cavemanFlagJob);
    
    return cavemanFlagJob;
  }
  
  private Job vcfUpload(String[] types, String controlAnalysisId, List<String> tumourAnalysisIds, List<String> tumourAliquotIds) {
    Job thisJob = getWorkflow().createBashJob("vcfUpload");
    
    String metadataUrls = new String();
    metadataUrls = metadataUrls.concat(gnosServer)
                              .concat("/cghub/metadata/analysisFull/")
                              .concat(controlAnalysisId);
    for(String tumourAnalysisId : tumourAnalysisIds) {
      metadataUrls = metadataUrls.concat(",")
                              .concat(gnosServer)
                              .concat("/cghub/metadata/analysisFull/")
                              .concat(tumourAnalysisId);
                              
    }
    
    String vcfs = new String();
    String tbis = new String();
    String tars = new String();
    String vcfmd5s = new String();
    String tbimd5s = new String();
    String tarmd5s = new String();
    for(String type: types) {
      for(String tumourAliquotId : tumourAliquotIds) {
        // TODO: Hardcoded somatic here, is that correct?
        String baseFile = OUTDIR + "/" + tumourAliquotId + "." + workflowName + "." + dateString + ".somatic." + type;
        if(vcfs.length() > 0) {
          vcfs = vcfs.concat(",");
          tbis = tbis.concat(",");
          tars = tars.concat(",");
          vcfmd5s = vcfmd5s.concat(",");
          tbimd5s = tbimd5s.concat(",");
          tarmd5s = tarmd5s.concat(",");
        }
        vcfs = vcfs.concat(baseFile + ".vcf.gz");
        tbis = tbis.concat(baseFile + ".vcf.gz.tbi");
        tars = tars.concat(baseFile + ".tar.gz");
        vcfmd5s = vcfmd5s.concat(baseFile + ".vcf.gz.md5");
        tbimd5s = tbimd5s.concat(baseFile + ".vcf.gz.tbi.md5");
        tarmd5s = tarmd5s.concat(baseFile + ".tar.gz.md5");
      }
    }
    
    thisJob.getCommand()
      .addArgument("perl " + getWorkflowBaseDir()+ "/bin/gnos_upload_vcf.pl")
      .addArgument("--metadata-urls " + metadataUrls)
      .addArgument("--vcfs " + vcfs)
      .addArgument("--vcf-md5sum-files " + vcfmd5s)
      .addArgument("--vcf-idxs " + tbis)
      .addArgument("--vcf-idx-md5sum-files " + tbimd5s)
      .addArgument("--tarballs " + tars)
      .addArgument("--tarball-md5sum-files " + tarmd5s)
      .addArgument("--outdir " + OUTDIR + "/upload")
      .addArgument("--key " + pemFile)
      .addArgument("--upload-url " + uploadServer)
      .addArgument("--qc-metrics-json " + OUTDIR + "/qc_metrics.json")
      .addArgument("--timing-metrics-json " + OUTDIR + "/process_metrics.json")
      ;
    try {
      if(hasPropertyAndNotNull("study-refname-override")) {
        thisJob.getCommand().addArgument("--study-refname-override " + getProperty("study-refname-override"));
      }
      if(hasPropertyAndNotNull("analysis-center-override")) {
        thisJob.getCommand().addArgument("--analysis-center-override " + getProperty("analysis-center-override"));
      }
      if(hasPropertyAndNotNull("upload-test") && Boolean.valueOf(getProperty("upload-test"))) {
        thisJob.getCommand().addArgument("--test ");
      }
      if(hasPropertyAndNotNull("upload-skip")) {
        thisJob.getCommand().addArgument("--skip-upload");
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    return thisJob;
  }
  
  private Job packageResults(int tumourCount, String algName, String resultType, String tumourBam, String baseVcf, String workflowName, String somaticOrGermline, String date) {
    //#packageResults.pl outdir 0772aed3-4df7-403f-802a-808df2935cd1/c007f362d965b32174ec030825262714.bam outdir/caveman snv_mnv flagged.muts.vcf.gz
    Job thisJob = getWorkflow().createBashJob("packageResults");
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument(getWorkflowBaseDir() + "/bin/packageResults.pl")
              .addArgument(OUTDIR)
              .addArgument(tumourBam)
              .addArgument(OUTDIR + "/" + tumourCount + "/" + algName)
              .addArgument(resultType)
              .addArgument(baseVcf)
              .addArgument(workflowName)
              .addArgument(somaticOrGermline)
              .addArgument(date)
      ;
    return thisJob;
  }
  
  private Job gnosDownloadBaseJob(String analysisId) {
    Job thisJob = getWorkflow().createBashJob("GNOSDownload");
    thisJob.getCommand()
                  .addArgument("gtdownload -c " + pemFile)
                  .addArgument("-v " + gnosServer + "/cghub/data/analysis/download/" + analysisId);
    return thisJob;
  }
  
  private Job basFileBaseJob(String analysisId, String sampleBam) {
    Job thisJob = getWorkflow().createBashJob("basFileGet");
    thisJob.getCommand()
            .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
            .addArgument(installBase)
            .addArgument("xml_to_bas.pl")
            .addArgument("-d " + gnosServer + "/cghub/metadata/analysisFull/" + analysisId)
            .addArgument("-o " + analysisId + "/" + sampleBam + ".bas")
            ;
    return thisJob;
  }
  
  private Job getMetricsJob(List<String> tumourBams) {
    //die "USAGE: rootOfOutdir ordered.bam [ordered.bam2]";
    Job thisJob = getWorkflow().createBashJob("metrics");
    thisJob.getCommand()
      .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
      .addArgument(installBase)
      .addArgument(getWorkflowBaseDir()+ "/bin/qc_and_metrics.pl")
      .addArgument(OUTDIR);
    for(String bam : tumourBams) {
      thisJob.getCommand().addArgument(bam);
    }
    return thisJob;
  }
  
  private Job caveCnPrep(int tumourCount, String type) {
    String cnPath = OUTDIR + "/" + tumourCount + "/ascat/*.copynumber.caveman.csv";
    
    Job thisJob = getWorkflow().createBashJob("CaveCnPrep" + type);
    int offset = 0;
    if(type.equals("tumour")) {
      offset = 6;
    }
    else if(type.equals("normal")) {
      offset = 4;
    }
    thisJob.getCommand()
      .addArgument("perl -ne '@F=(split q{,}, $_)[1,2,3," + offset + "]; $F[1]-1; print join(\"\\t\",@F).\"\\n\";'")
      .addArgument("< " + cnPath)
      .addArgument("> " + OUTDIR + "/" + tumourCount + "/" + type + ".cn.bed")
      ;
    thisJob.setMaxMemory(memCaveCnPrep);
    return thisJob;
  }
  
  private Job cavemanTbiCleanJob() {
    Job thisJob = getWorkflow().createBashJob("CaveTbiClean");
      thisJob.getCommand().addArgument("rm -f " + OUTDIR + "/unmatchedNormal.*.vcf.gz.tbi");
    return thisJob;
  }
  
  private Job postUploadCleanJob() {
    Job thisJob = getWorkflow().createBashJob("postUploadClean");
    thisJob.getCommand().addArgument("rm -rf ./*/*.bam " + OUTDIR);
    return thisJob;
  }

  private Job cleanInputsJob() {
    Job thisJob = getWorkflow().createBashJob("cleanInputs");
    thisJob.getCommand().addArgument("rm -f ./*/*.bam");
    return thisJob;
  }
  
  private Job cavemanBaseJob(int tumourCount, String tumourBam, String controlBam, String alg, String process, int index) {
    String ascatContamFile = OUTDIR + "/" + tumourCount + "/ascat/*.samplestatistics.csv";
    
    Job thisJob = prepTimedJob(tumourCount, alg, process, index);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument("caveman.pl")
              .addArgument("-p " + process)
              .addArgument("-i " + index)
              .addArgument("-ig " + refBase + "/caveman/ucscHiDepth_0.01_merge1000_no_exon.tsv")
              .addArgument("-b " + refBase + "/caveman/flagging")
              .addArgument("-u " + tabixSrvUri)
              .addArgument("-np " + seqType)
              .addArgument("-tp " + seqType)
              .addArgument("-sa " + assembly)
              .addArgument("-s " + species)
              .addArgument("-st " + seqProtocol)
              .addArgument("-o " + OUTDIR + "/" + tumourCount + "/caveman")
              .addArgument("-tc " + OUTDIR + "/" + tumourCount + "/tumour.cn.bed")
              .addArgument("-nc " + OUTDIR + "/" + tumourCount + "/normal.cn.bed")
              .addArgument("-k " + ascatContamFile);
    if(tumourBam.startsWith("/")) {
      thisJob.getCommand().addArgument("-tb " + tumourBam)
                          .addArgument("-nb " + controlBam)
                          .addArgument("-r " + genomeFaGz + ".fai");
    }
    else {
      thisJob.getCommand().addArgument("-tb " + tumourBam)
                          .addArgument("-nb " + controlBam)
                          .addArgument("-r " + genomeFaGz + ".fai");
    }
    
    if(process.equals("mstep") || process.equals("estep")) {
      thisJob.getCommand().addArgument("-l " + coresAddressable);
    }
    else if(process.equals("flag")) {
      thisJob.getCommand().addArgument("-in " + OUTDIR + "/" + tumourCount + "/pindel/*.germline.bed");
    }

    return thisJob;
  }

  private Job cgpAscatBaseJob(int tumourCount, String tumourBam, String controlBam, String alg, String process, int index) {
    Job thisJob = prepTimedJob(tumourCount, alg, process, index);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument("ascat.pl")
              .addArgument("-p " + process)
              .addArgument("-i " + index)
              .addArgument("-r " + genomeFaGz)
              .addArgument("-s " + refBase + "/ascat/SnpLocus.tsv")
              .addArgument("-sp " + refBase + "/ascat/SnpPositions.tsv")
              .addArgument("-sg " + refBase + "/ascat/SnpGcCorrections.tsv")
              .addArgument("-pr " + seqType)
              .addArgument("-ra " + assembly)
              .addArgument("-rs " + species)
              .addArgument("-pl " + "ILLUMINA") // should be in BAM header
              .addArgument("-o " + OUTDIR + "/" + tumourCount + "/ascat")
              .addArgument("-t " + tumourBam)
              .addArgument("-n " + controlBam)
              .addArgument("-f") // force completion, even when ascat fails
              ;
    // this is used when gender is not specified
    if(gender.equals("L")) {
      thisJob.getCommand().addArgument("-l Y:2654896-2655740");
    }
    thisJob.getCommand().addArgument("-g " + gender);

    return thisJob;
  }

  private Job pindelBaseJob(int tumourCount, String tumourBam, String controlBam, String alg, String process, int index) {
    Job thisJob = prepTimedJob(tumourCount, alg, process, index);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument("pindel.pl")
              .addArgument("-p " + process)
              .addArgument("-i " + index)
              .addArgument("-r " + genomeFaGz)
              .addArgument("-e " + refExclude)
              .addArgument("-st " + seqType)
              .addArgument("-as " + assembly)
              .addArgument("-sp " + species)
              .addArgument("-s " + refBase + "/pindel/simpleRepeats.bed.gz")
              .addArgument("-f " + refBase + "/pindel/genomicRules.lst")
              .addArgument("-g " + refBase + "/vagrent/e74/Human.GRCh37.codingexon_regions.indel.bed.gz")
              .addArgument("-u " + refBase + "/pindel/pindel_np.gff3.gz")
              .addArgument("-sf " + refBase + "/pindel/softRules.lst")
              .addArgument("-b " + refBase + "/shared/ucscHiDepth_0.01_mrg1000_no_exon_coreChrs.bed.gz")
              .addArgument("-o " + OUTDIR + "/" + tumourCount + "/pindel")
              .addArgument("-t " + tumourBam)
              .addArgument("-n " + controlBam)
              ;
    if(process.equals("input")) {
      int pindelInputThreads;
      if(coresAddressable > 4) {
        pindelInputThreads = 4;
      }
      else {
        pindelInputThreads = coresAddressable;
      }
      thisJob.getCommand().addArgument("-c " + pindelInputThreads);
      thisJob.setThreads(pindelInputThreads);
    }
    return thisJob;
  }
  
  private Job markTime(String item) {
    String timeFile = TIMEDIR + "/workflow_" + item;
    Job thisJob = getWorkflow().createBashJob("mark_" + item);
    thisJob.getCommand().addArgument("date +%s > " + timeFile);
    return thisJob;
  }
  
  private Job stageTbi() {
    Job thisJob = getWorkflow().createBashJob("getTbi");
    thisJob.getCommand()
      .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
      .addArgument(installBase)
      .addArgument("getTbi.pl")
      .addArgument(genomeFaGz + ".fai")
      .addArgument(tabixSrvUri)
      ;
    return thisJob;
  }

  private Job prepTimedJob(int tumourCount, String alg, String process, int index) {
    String timeFile = TIMEDIR + "/" + tumourCount + "_" + alg + "_" + process + "_" + index;
    Job thisJob = getWorkflow().createBashJob(alg + "_" + process);
    thisJob.getCommand().addArgument("/usr/bin/time /usr/bin/time --format=\"Wall_s %e\\nUser_s %U\\nSystem_s %S\\nMax_kb %M\" --output=" + timeFile);
    return thisJob;
  }
  
  private Job brassBaseJob(int tumourCount, String tumourBam, String controlBam, String alg, String process, int index) {
    Job thisJob = prepTimedJob(tumourCount, alg, process, index);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument("brass.pl")
              .addArgument("-p " + process)
              .addArgument("-i " + index)
              .addArgument("-g " + genomeFaGz)
              .addArgument("-e " + refExclude)
              .addArgument("-pr " + seqType)
              .addArgument("-as " + assembly)
              .addArgument("-s " + species)
              .addArgument("-pl " + "ILLUMINA") // should be in BAM header
              .addArgument("-d "  + refBase + "/shared/ucscHiDepth_0.01_mrg1000_no_exon_coreChrs.bed.gz")
              .addArgument("-r "  + refBase + "/brass/brassRepeats.bed.gz")
              .addArgument("-f "  + refBase + "/brass/brass_np.groups.gz")
              .addArgument("-g_cache "  + refBase + "/vagrent/e74/Homo_sapiens.GRCh37.74.vagrent.cache.gz")
              .addArgument("-o " + OUTDIR + "/" + tumourCount + "/brass")
              .addArgument("-t " + tumourBam)
              .addArgument("-n " + controlBam)
            ;
    if(process.equals("filter")) {
      String cnPath = OUTDIR + "/" + tumourCount + "/ascat/*.copynumber.caveman.csv";
      thisJob.getCommand().addArgument("-a " + cnPath);
    }
    else if(process.endsWith("assemble")) {
      thisJob.getCommand().addArgument("-l " + coresAddressable);
    }
    return thisJob;
  }

}
