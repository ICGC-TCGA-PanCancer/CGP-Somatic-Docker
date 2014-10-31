package io.seqware.pancancer;

import java.util.ArrayList;
import java.util.Arrays;
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
 */
public class CgpCnIndelSnvStrWorkflow extends AbstractWorkflowDataModel {

  private boolean manualOutput=false;
  private String catPath, echoPath;
  private String greeting ="";
  private static final String OUTDIR = "outdir";
  
  private boolean testMode=false;

  // MEMORY variables //
  private String  memBasFileGet, memGnosDownload, memPackageResults, memUpload,
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
  
  private int pindelInputThreads, coresAddressable;
  
  private void init() {
    try {
      //optional properties
      if (hasPropertyAndNotNull("manual_output")) {
        manualOutput = Boolean.valueOf(getProperty("manual_output"));
      }
      if (hasPropertyAndNotNull("greeting")) {
        greeting = getProperty("greeting");
      }
      //these two properties are essential to the workflow. If they are null or do not
      //exist in the INI, the workflow should exit.
      catPath = getProperty("cat");
      echoPath = getProperty("echo");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setupDirectory() {
    //since setupDirectory is the first method run, we use it to initialize variables too.
    init();
    // creates a dir1 directory in the current working directory where the workflow runs
    addDirectory(OUTDIR);
  }

  @Override
  public Map<String, SqwFile> setupFiles() {
    try {
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
      memUpload = getProperty("memUpload");
      
      
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
      
      // Specific to PINDEL workflow //
      pindelInputThreads = Integer.valueOf(getProperty("pindelInputThreads"));
      if(coresAddressable < pindelInputThreads) {
        pindelInputThreads = 1;
      }
      
      // Specific to Caveman workflow //
      tabixSrvUri = getProperty("tabixSrvUri");

      // pindel specific
      refExclude = getProperty("refExclude");

      // test mode
      if(!testMode) {
        gnosServer = getProperty("gnosServer");
        pemFile = getProperty("pemFile");
      }

      //environment
      installBase = getWorkflowBaseDir() + "/bin/opt";
      refBase = getWorkflowBaseDir() + "/cgp_reference";
      genomeFaGz = getWorkflowBaseDir() + "/data/reference/genome.fa.gz";
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    return getFiles();
  }
  
  private Job bamProvision(String analysisId, String bamFile) {
    Job basJob = null;
    if(testMode == false) {
      Job gnosDownload = gnosDownloadBaseJob(analysisId);
      gnosDownload.setMaxMemory(memGnosDownload);
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
    Job controlBasJob = null;
    String controlBam;
    List<String> tumourBams = new ArrayList<String>();
    List<Job> tumourBasJobs = new ArrayList<Job>();
    
    List<String> tumourAnalysisIds = new ArrayList<String>();
    List<String> tumourAliquotIds = new ArrayList<String>();
    String controlAnalysisId = new String();
    
    try {
      if(testMode) {
        controlBam = getProperty("controlBamT");
        tumourBams = Arrays.asList(getProperty("tumourBamT").split(":"));
        for(String t : tumourBams) {
          tumourBasJobs.add(null);
        }
      }
      else {
        controlAnalysisId = getProperty("controlAnalysisId");
        controlBasJob = bamProvision(controlAnalysisId, getProperty("controlBam"));
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
          Job tumourBasJob = bamProvision(tumourAnalysisIds.get(i), rawBams.get(i));
          tumourBasJobs.add(tumourBasJob);
          String tumourBam = tumourAnalysisIds.get(i) + "/" + rawBams.get(i);
          tumourBams.add(tumourBam);
        }
      }
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
    
    Job[] cavemanFlagJobs = new Job [tumourBams.size()];
    for(int i=0; i<tumourBams.size(); i++) {
      Job cavemanFlagJob = buildPairWorkflow(controlBasJob, tumourBasJobs.get(i), controlBam, tumourBams.get(i), i);
      cavemanFlagJobs[i] = cavemanFlagJob;
    }
    
    Job cavemanTbiCleanJob = cavemanTbiCleanJob();
    cavemanTbiCleanJob.setMaxMemory(memCavemanTbiClean);
    for(Job cavemanFlagJob : cavemanFlagJobs) {
      cavemanTbiCleanJob.addParent(cavemanFlagJob);
    }
    
    if(uploadServer != null) {
      String[] resultTypes = {"snv_mnv","cnv","sv","indel"};
      Job uploadJob = vcfUpload(resultTypes, controlAnalysisId, tumourAnalysisIds, tumourAliquotIds);
      uploadJob.setMaxMemory(memUpload);
      uploadJob.addParent(cavemanTbiCleanJob);
    }
  }

  /**
   * This builds the workflow for a pair of samples
   * The generic buildWorkflow section will choose the pair to be processed and 
   * setup the control sample download
   */
  private Job buildPairWorkflow(Job controlBasJob, Job tumourBasJob, String controlBam, String tumourBam, int tumourCount) {
    
    /**
     * ASCAT - Copynumber
     * Depends on
     *  - tumour/control BAMs
     *  - Gender, will attempt to determine if not specified
     */

    Job[] alleleCountJobs = new Job[2];
    for(int i=0; i<2; i++) {
      Job alleleCountJob = cgpAscatBaseJob(tumourCount, tumourBam, controlBam, "ascatAlleleCount", "allele_count", i+1);
      alleleCountJob.setMaxMemory(memAlleleCount);
      if(testMode == false) {
        alleleCountJob.addParent(controlBasJob);
        alleleCountJob.addParent(tumourBasJob);
      }
      alleleCountJobs[i] = alleleCountJob;
    }

    Job ascatJob = cgpAscatBaseJob(tumourCount, tumourBam, controlBam, "ascat", "ascat", 1);
    ascatJob.setMaxMemory(memAscat);
    ascatJob.addParent(alleleCountJobs[0]);
    ascatJob.addParent(alleleCountJobs[1]);
    
    Job ascatFinaliseJob = cgpAscatBaseJob(tumourCount, tumourBam, controlBam, "ascatFinalise", "finalise", 1);
    ascatFinaliseJob.setMaxMemory(memAscatFinalise);
    ascatFinaliseJob.addParent(ascatJob);
    
    Job ascatPackage = packageResults(tumourCount, "ascat", "cnv", tumourBam, "copynumber.caveman.vcf.gz");
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
       if(testMode == false) {
        caveCnPrepJob.addParent(controlBasJob);
        caveCnPrepJob.addParent(tumourBasJob);
      }
      caveCnPrepJob.addParent(ascatFinaliseJob); // ASCAT dependency!!!
      caveCnPrepJobs[i] = caveCnPrepJob;
    }
    
    
    Job cavemanSetupJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "cavemanSetup", "setup", 1);
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
      Job inputParse = pindelBaseJob(tumourCount, tumourBam, controlBam, "pindelInput", "input", i+1);
      inputParse.setMaxMemory(memPindelInput);
      if(testMode == false) {
        inputParse.addParent(controlBasJob);
        inputParse.addParent(tumourBasJob);
      }
      pindelInputJobs[i] = inputParse;
    }
    
    // determine number of refs to process
    // we know that this is static for PanCancer so be lazy 24 jobs (1-22,X,Y)
    // but pindel needs to know the exclude list so hard code this
    Job pinVcfJobs[] = new Job[24];
    for(int i=0; i<24; i++) {
      Job pindelJob = pindelBaseJob(tumourCount, tumourBam, controlBam, "pindelPindel", "pindel", i+1);
      pindelJob.setMaxMemory(memPindel);
      pindelJob.addParent(pindelInputJobs[0]);
      pindelJob.addParent(pindelInputJobs[1]);
      
      Job pinVcfJob = pindelBaseJob(tumourCount, tumourBam, controlBam, "pindelVcf", "pin2vcf", i+1);
      pinVcfJob.setMaxMemory(memPindelVcf);
      pinVcfJob.addParent(pindelJob);
      
      // pinVcf depends on pindelJob so only need have dependency on the pinVcf
      pinVcfJobs[i] = pinVcfJob;
    }
    
    Job pindelMergeJob = pindelBaseJob(tumourCount, tumourBam, controlBam, "pindelMerge", "merge", 1);
    pindelMergeJob.setMaxMemory(memPindelMerge);
    for (Job parent : pinVcfJobs) {
      pindelMergeJob.addParent(parent);
    }
    
    Job pindelFlagJob = pindelBaseJob(tumourCount, tumourBam, controlBam, "pindelFlag", "flag", 1);
    pindelFlagJob.setMaxMemory(memPindelFlag);
    pindelFlagJob.addParent(pindelMergeJob);
    pindelFlagJob.addParent(cavemanSetupJob);
    
    Job pindelPackage = packageResults(tumourCount, "pindel", "indel", tumourBam, "flagged.vcf.gz");
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
      Job brassInputJob = brassBaseJob(tumourCount, tumourBam, controlBam, "brassInput", "input", i+1);
      brassInputJob.setMaxMemory(memBrassInput);
      if(testMode == false) {
        brassInputJob.addParent(controlBasJob);
        brassInputJob.addParent(tumourBasJob);
      }
      brassInputJobs[i] = brassInputJob;
    }
    
    Job brassGroupJob = brassBaseJob(tumourCount, tumourBam, controlBam, "brassGroup", "group", 1);
    brassGroupJob.setMaxMemory(memBrassGroup);
    brassGroupJob.addParent(brassInputJobs[0]);
    brassGroupJob.addParent(brassInputJobs[1]);
    
    Job brassFilterJob = brassBaseJob(tumourCount, tumourBam, controlBam, "brassFilter", "filter", 1);
    brassFilterJob.setMaxMemory(memBrassFilter);
    brassFilterJob.addParent(brassGroupJob);
    brassFilterJob.addParent(ascatFinaliseJob); // NOTE: dependency on ASCAT!!
    
    Job brassSplitJob = brassBaseJob(tumourCount, tumourBam, controlBam, "brassSplit", "split", 1);
    brassSplitJob.setMaxMemory(memBrassSplit);
    brassSplitJob.addParent(brassFilterJob);
    
    List<Job> brassAssembleJobs = new ArrayList<Job>();
    for(int i=0; i<coresAddressable; i++) {
      Job brassAssembleJob = brassBaseJob(tumourCount, tumourBam, controlBam, "brassAssemble", "assemble", i+1);
      brassAssembleJob.setMaxMemory(memBrassAssemble);
      brassAssembleJob.addParent(brassSplitJob);
      brassAssembleJobs.add(brassAssembleJob);
    }
    
    Job brassGrassJob = brassBaseJob(tumourCount, tumourBam, controlBam, "brassGrass", "grass", 1);
    brassGrassJob.setMaxMemory(memBrassGrass);
    for(Job brassAssembleJob : brassAssembleJobs) {
      brassGrassJob.addParent(brassAssembleJob);
    }
    
    Job brassTabixJob = brassBaseJob(tumourCount, tumourBam, controlBam, "brassTabix", "tabix", 1);
    brassTabixJob.setMaxMemory(memBrassTabix);
    brassTabixJob.addParent(brassGrassJob);
    
    Job brassPackage = packageResults(tumourCount, "brass", "sv", tumourBam, "annot.vcf.gz");
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
      Job cavemanSplitJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "cavemanSplit", "split", i+1);
      cavemanSplitJob.setMaxMemory(memCavemanSplit);
      cavemanSplitJob.addParent(cavemanSetupJob);
      cavemanSplitJobs[i] = cavemanSplitJob;
    }
    
    Job cavemanSplitConcatJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "cavemanSplitConcat", "split_concat", 1);
    cavemanSplitConcatJob.setMaxMemory(memCavemanSplitConcat);
    for (Job cavemanSplitJob : cavemanSplitJobs) {
      cavemanSplitConcatJob.addParent(cavemanSplitJob);
    }
    
    List<Job> cavemanMstepJobs = new ArrayList<Job>();
    for(int i=0; i<coresAddressable; i++) {
      Job cavemanMstepJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "cavemanMstep", "mstep", i+1);
      cavemanMstepJob.setMaxMemory(memCavemanMstep);
      cavemanMstepJob.addParent(cavemanSplitConcatJob);
      cavemanMstepJobs.add(cavemanMstepJob);
    }
    
    Job cavemanMergeJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "cavemanMerge", "merge", 1);
    cavemanMergeJob.setMaxMemory(memCavemanMerge);
    for(Job cavemanMstepJob : cavemanMstepJobs) {
      cavemanMergeJob.addParent(cavemanMstepJob);
    }
    
    List<Job> cavemanEstepJobs = new ArrayList<Job>();
    for(int i=0; i<coresAddressable; i++) {
      Job cavemanEstepJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "cavemanEstep", "estep", i+1);
      cavemanEstepJob.setMaxMemory(memCavemanEstep);
      cavemanEstepJob.addParent(cavemanMergeJob);
      cavemanEstepJobs.add(cavemanEstepJob);
    }
    
    Job cavemanMergeResultsJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "cavemanMergeResults", "merge_results", 1);
    cavemanMergeResultsJob.setMaxMemory(memCavemanMergeResults);
    for(Job cavemanEstepJob : cavemanEstepJobs) {
      cavemanMergeResultsJob.addParent(cavemanEstepJob);
    }
    
    Job cavemanAddIdsJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "cavemanAddIds", "add_ids", 1);
    cavemanAddIdsJob.setMaxMemory(memCavemanAddIds);
    cavemanAddIdsJob.addParent(cavemanMergeResultsJob);
    
    Job cavemanFlagJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "cavemanFlag", "flag", 1);
    cavemanFlagJob.setMaxMemory(memCavemanFlag);
    cavemanFlagJob.addParent(pindelFlagJob); // PINDEL dependency
    cavemanFlagJob.addParent(cavemanAddIdsJob);
    
    Job cavemanPackage = packageResults(tumourCount, "caveman", "snv_mnv", tumourBam, "flagged.muts.vcf.gz");
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
        String baseFile = OUTDIR + "/" + tumourAliquotId + "_" + type;
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
      .addArgument("gnos_upload_vcf.pl")
      .addArgument("--metadata-urls " + metadataUrls)
      .addArgument("--vcfs " + vcfs)
      .addArgument("--vcf-md5sum-files" + vcfmd5s)
      .addArgument("--vcf-idxs " + tbis)
      .addArgument("--vcf-idx-md5sum-files " + tbimd5s)
      .addArgument("--tarballs " + tars)
      .addArgument("--tarball-md5sum-files " + tarmd5s)
      .addArgument("--outdir " + OUTDIR + "/upload")
      .addArgument("--key " + pemFile)
      .addArgument("--upload-url " + gnosServer)
      ;
    try {
      if(hasPropertyAndNotNull("study-refname-override")) {
        thisJob.getCommand().addArgument("--study-refname-override " + getProperty("study-refname-override"));
      }
      if(hasPropertyAndNotNull("analysis-center-override")) {
        thisJob.getCommand().addArgument("--analysis-center-override " + getProperty("analysis-center-override"));
      }
      
      if(hasPropertyAndNotNull("upload-test")) {
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
  
  private Job packageResults(int tumourCount, String algName, String resultType, String tumourBam, String baseVcf) {
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
            .addArgument("-o " + sampleBam + ".bas")
            ;
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
      thisJob.getCommand().addArgument("rm -f " + OUTDIR + "/*/unmatchedNormal.*.vcf.gz.tbi");
    return thisJob;
  }
  
  private Job cavemanBaseJob(int tumourCount, String tumourBam, String controlBam, String name, String process, int index) {
    String ascatContamFile = OUTDIR + "/" + tumourCount + "/ascat/*.samplestatistics.csv";
    
    Job thisJob = getWorkflow().createBashJob(name);
    String prependLoc = "";
    if(name.equals("cavemanFlag")) {
      // very simplistic way to get round clash of tabix file downloads
      thisJob.getCommand().addArgument("cd " + OUTDIR + "/" + tumourCount + ";");
      prependLoc = "../../"; // but as we don't have absoulte path need to 
    }
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument("caveman.pl")
              .addArgument("-p " + process)
              .addArgument("-i " + index)
            
              .addArgument("-r " + prependLoc + genomeFaGz + ".fai")
              .addArgument("-ig " + refBase + "/caveman/ucscHiDepth_0.01_merge1000_no_exon.tsv")
              .addArgument("-b " + refBase + "/caveman/flagging")
              .addArgument("-u " + tabixSrvUri)
              .addArgument("-np " + seqType)
              .addArgument("-tp " + seqType)
              .addArgument("-sa " + assembly)
              .addArgument("-s " + species)
              .addArgument("-st " + seqProtocol)
            
              .addArgument("-o " + prependLoc + OUTDIR + "/" + tumourCount + "/caveman")
              .addArgument("-tc " + prependLoc + OUTDIR + "/" + tumourCount + "/tumour.cn.bed")
              .addArgument("-nc " + prependLoc + OUTDIR + "/" + tumourCount + "/normal.cn.bed")
              .addArgument("-k " + prependLoc + ascatContamFile);
    if(tumourBam.startsWith("/")) {
      thisJob.getCommand().addArgument("-tb " + tumourBam)
                          .addArgument("-nb " + controlBam);
    }
    else {
      thisJob.getCommand().addArgument("-tb " + prependLoc + tumourBam)
                          .addArgument("-nb " + prependLoc + controlBam);
    }
    
    if(name.equals("cavemanMstep") || name.equals("cavemanEstep")) {
      thisJob.getCommand().addArgument("-l " + coresAddressable);
    }
    else if(name.equals("cavemanFlag")) {
      thisJob.getCommand().addArgument("-in " + prependLoc + OUTDIR + "/" + tumourCount + "/pindel/*.germline.bed");
    }

    return thisJob;
  }

  private Job cgpAscatBaseJob(int tumourCount, String tumourBam, String controlBam, String name, String process, int index) {
    Job thisJob = getWorkflow().createBashJob(name);
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

  private Job pindelBaseJob(int tumourCount, String tumourBam, String controlBam, String name, String process, int index) {
    Job thisJob = getWorkflow().createBashJob(name);
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
    if(name.equals("pindelInput")) {
      thisJob.getCommand().addArgument("-c " + pindelInputThreads);
      thisJob.setThreads(pindelInputThreads);
    }
    return thisJob;
  }

  private Job brassBaseJob(int tumourCount, String tumourBam, String controlBam, String name, String process, int index) {
    Job thisJob = getWorkflow().createBashJob(name);
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
    if(name.equals("brassFilter")) {
      String cnPath = OUTDIR + "/" + tumourCount + "/ascat/*.copynumber.caveman.csv";
      thisJob.getCommand().addArgument("-a " + cnPath);
    }
    else if(name.endsWith("brassAssemble")) {
      thisJob.getCommand().addArgument("-l " + coresAddressable);
    }
    return thisJob;
  }

}
