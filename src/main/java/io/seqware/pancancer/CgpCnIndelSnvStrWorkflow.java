package io.seqware.pancancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.sourceforge.seqware.pipeline.workflowV2.AbstractWorkflowDataModel;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;

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
  private static final String LOGDIR = "logdir/"; // leave trailing slash on this
  
  private boolean testMode=false;

  // MEMORY variables //
  private String  memBasFileGet, memGnosDownload, memPackageResults,
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
                  tumourAnalysisId, controlAnalysisId, pemFile, gnosServer,
                  // test files, instead of GNOS ids
                  tumourBam, controlBam,
                  // ascat variables
                  gender, ascatCn, ascatContam,
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
    addDirectory(LOGDIR);
  }

  @Override
  public Map<String, SqwFile> setupFiles() {
    try {
      if(hasPropertyAndNotNull("testMode")) {
        testMode=Boolean.valueOf(getProperty("testMode"));
      }
      
      // used by steps that can use all available cores
      coresAddressable = Integer.valueOf(getProperty("coresAddressable"));

      // MEMORY //
      memBasFileGet = getProperty("memBasFileGet");
      memGnosDownload = getProperty("memGnosDownload");
      memPackageResults = getProperty("memPackageResults");
      
      
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
      if(testMode) {
        tumourBam = getProperty("tumourBamT");
        controlBam = getProperty("controlBamT");
        if(hasPropertyAndNotNull("pindelGermline")) {
          pindelGermline = getProperty("pindelGermline");
        }
        if(hasPropertyAndNotNull("ascatCn")) {
          ascatCn = getProperty("ascatCn");
        }
        if(hasPropertyAndNotNull("ascatContam")) {
          ascatContam = getProperty("ascatContam");
        }
      }
      else {
        tumourAnalysisId = getProperty("tumourAnalysisId");
        controlAnalysisId = getProperty("controlAnalysisId");
        gnosServer = getProperty("gnosServer");
        pemFile = getProperty("pemFile");
        tumourBam = tumourAnalysisId + "/" + getProperty("tumourBam");
        controlBam = controlAnalysisId + "/" + getProperty("controlBam");
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

  @Override
  public void buildWorkflow() {
    
    // First we need the tumour and normal BAM files (+bai)
    // this can be done in parallel, based on tumour/control
    Job[] gnosDownloadJobs = new Job[2];
    Job[] basDownloadJobs = new Job[2];
    
    // @TODO, when we have a decider in place
    if(testMode == false) {
      for(int i=0; i<2; i++) {
        String thisId, thisBam;
        if(i == 0) {
          thisId = tumourAnalysisId;
          thisBam = tumourBam;
        }
        else {
          thisId = controlAnalysisId;
          thisBam = controlBam;
        }
        
        Job gnosDownload = gnosDownloadBaseJob(thisId);
        gnosDownload.setMaxMemory(memGnosDownload);
        // the file needs to end up in tumourBam/normalBam
        gnosDownloadJobs[i] = gnosDownload;

        // get the BAS files
        Job basJob = basFileBaseJob(thisId, thisBam);
        basJob.setMaxMemory(memBasFileGet);
        basJob.addParent(gnosDownload);
        basDownloadJobs[i] = basJob;
      }
    }
    
    /**
     * ASCAT - Copynumber
     * Depends on
     *  - tumour/normal BAMs
     *  - Gender, will attempt to determine if not specified
     */

    Job[] alleleCountJobs = new Job[2];
    for(int i=0; i<2; i++) {
      Job alleleCountJob = cgpAscatBaseJob("ascatAlleleCount", "allele_count", i+1);
      alleleCountJob.setMaxMemory(memAlleleCount);
      if(testMode == false) {
        alleleCountJob.addParent(basDownloadJobs[0]);
        alleleCountJob.addParent(basDownloadJobs[1]);
      }
      alleleCountJobs[i] = alleleCountJob;
    }

    Job ascatJob = cgpAscatBaseJob("ascat", "ascat", 1);
    ascatJob.setMaxMemory(memAscat);
    ascatJob.addParent(alleleCountJobs[0]);
    ascatJob.addParent(alleleCountJobs[1]);
    
    Job ascatFinaliseJob = cgpAscatBaseJob("ascatFinalise", "finalise", 1);
    ascatFinaliseJob.setMaxMemory(memAscatFinalise);
    ascatFinaliseJob.addParent(ascatJob);
    
    Job ascatPackage = packageResults("ascat", "cnv", tumourBam, "copynumber.caveman.vcf.gz");
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
        caveCnPrepJob = caveCnPrep("tumour");
      }
      else {
        caveCnPrepJob = caveCnPrep("normal");
      }
       if(testMode == false) {
        caveCnPrepJob.addParent(basDownloadJobs[0]);
        caveCnPrepJob.addParent(basDownloadJobs[1]);
      }
      caveCnPrepJob.addParent(ascatFinaliseJob); // ASCAT dependency!!!
      caveCnPrepJobs[i] = caveCnPrepJob;
    }
    
    
    Job cavemanSetupJob = cavemanBaseJob("cavemanSetup", "setup", 1);
    cavemanSetupJob.setMaxMemory(memCavemanSetup);
    cavemanSetupJob.addParent(caveCnPrepJobs[0]);
    cavemanSetupJob.addParent(caveCnPrepJobs[1]);
    // some dependencies handled by ascat step
  
    /**
     * Pindel - InDel calling
     * Depends on:
     *  - tumour/normal BAMs
     */
     
    Job[] pindelInputJobs = new Job[2];
    for(int i=0; i<2; i++) {
      Job inputParse = pindelBaseJob("pindelInput", "input", i+1);
      inputParse.setMaxMemory(memPindelInput);
      if(testMode == false) {
        inputParse.addParent(basDownloadJobs[0]);
        inputParse.addParent(basDownloadJobs[1]);
      }
      pindelInputJobs[i] = inputParse;
    }
    
    // determine number of refs to process
    // we know that this is static for PanCancer so be lazy 24 jobs (1-22,X,Y)
    // but pindel needs to know the exclude list so hard code this
    Job pinVcfJobs[] = new Job[24];
    for(int i=0; i<24; i++) {
      Job pindelJob = pindelBaseJob("pindelPindel", "pindel", i+1);
      pindelJob.setMaxMemory(memPindel);
      pindelJob.addParent(pindelInputJobs[0]);
      pindelJob.addParent(pindelInputJobs[1]);
      
      Job pinVcfJob = pindelBaseJob("pindelVcf", "pin2vcf", i+1);
      pinVcfJob.setMaxMemory(memPindelVcf);
      pinVcfJob.addParent(pindelJob);
      
      // pinVcf depends on pindelJob so only need have dependency on the pinVcf
      pinVcfJobs[i] = pinVcfJob;
    }
    
    Job pindelMergeJob = pindelBaseJob("pindelMerge", "merge", 1);
    pindelMergeJob.setMaxMemory(memPindelMerge);
    for (Job parent : pinVcfJobs) {
      pindelMergeJob.addParent(parent);
    }
    
    Job pindelFlagJob = pindelBaseJob("pindelFlag", "flag", 1);
    pindelFlagJob.setMaxMemory(memPindelFlag);
    pindelFlagJob.addParent(pindelMergeJob);
    pindelFlagJob.addParent(cavemanSetupJob);
    
    Job pindelPackage = packageResults("pindel", "indel", tumourBam, "flagged.vcf.gz");
    pindelPackage.setMaxMemory(memPackageResults);
    pindelPackage.addParent(pindelFlagJob);
    
    /**
     * BRASS - BReakpoint AnalySiS
     * Depends on:
     *  - tumour/normal BAMs
     *  - ASCAT output at filter step
     */
    
    Job brassInputJobs[] = new Job[2];
    for(int i=0; i<2; i++) {
      Job brassInputJob = brassBaseJob("brassInput", "input", i+1);
      brassInputJob.setMaxMemory(memBrassInput);
      if(testMode == false) {
        brassInputJob.addParent(basDownloadJobs[0]);
        brassInputJob.addParent(basDownloadJobs[1]);
      }
      brassInputJobs[i] = brassInputJob;
    }
    
    Job brassGroupJob = brassBaseJob("brassGroup", "group", 1);
    brassGroupJob.setMaxMemory(memBrassGroup);
    brassGroupJob.addParent(brassInputJobs[0]);
    brassGroupJob.addParent(brassInputJobs[1]);
    
    Job brassFilterJob = brassBaseJob("brassFilter", "filter", 1);
    brassFilterJob.setMaxMemory(memBrassFilter);
    brassFilterJob.addParent(brassGroupJob);
    brassFilterJob.addParent(ascatFinaliseJob); // NOTE: dependency on ASCAT!!
    
    Job brassSplitJob = brassBaseJob("brassSplit", "split", 1);
    brassSplitJob.setMaxMemory(memBrassSplit);
    brassSplitJob.addParent(brassFilterJob);
    
    Job brassAssembleJob = brassBaseJob("brassAssemble", "assemble", 1);
    brassAssembleJob.setMaxMemory(memBrassAssemble);
    brassAssembleJob.addParent(brassSplitJob);
    
    Job brassGrassJob = brassBaseJob("brassGrass", "grass", 1);
    brassGrassJob.setMaxMemory(memBrassGrass);
    brassGrassJob.addParent(brassAssembleJob);
    
    Job brassTabixJob = brassBaseJob("brassTabix", "tabix", 1);
    brassTabixJob.setMaxMemory(memBrassTabix);
    brassTabixJob.addParent(brassGrassJob);
    
    Job brassPackage = packageResults("brass", "sv", tumourBam, "annot.vcf.gz");
    brassPackage.setMaxMemory(memPackageResults);
    brassPackage.addParent(brassTabixJob);
    
    
    /**
     * CaVEMan - SNV analysis
     * !! see above as setup done earlier to help with workflow structure !!
     * Depends on:
     *  - tumour/normal BAMs (but depend on BAS for better workflow)
     *  - ASCAT from outset
     *  - pindel at flag step
     */
    
    // should really line count the fai file
    Job cavemanSplitJobs[] = new Job[86];
    for(int i=0; i<86; i++) {
      Job cavemanSplitJob = cavemanBaseJob("cavemanSplit", "split", i+1);
      cavemanSplitJob.setMaxMemory(memCavemanSplit);
      cavemanSplitJob.addParent(cavemanSetupJob);
      cavemanSplitJobs[i] = cavemanSplitJob;
    }
    
    Job cavemanSplitConcatJob = cavemanBaseJob("cavemanSplitConcat", "split_concat", 1);
    cavemanSplitConcatJob.setMaxMemory(memCavemanSplitConcat);
    for (Job cavemanSplitJob : cavemanSplitJobs) {
      cavemanSplitConcatJob.addParent(cavemanSplitJob);
    }
    
    List<Job> cavemanMstepJobs = new ArrayList<Job>();
    for(int i=0; i<coresAddressable; i++) {
      Job cavemanMstepJob = cavemanBaseJob("cavemanMstep", "mstep", i+1);
      cavemanMstepJob.setMaxMemory(memCavemanMstep);
      cavemanMstepJob.addParent(cavemanSplitConcatJob);
      cavemanMstepJobs.add(cavemanMstepJob);
    }
    
    Job cavemanMergeJob = cavemanBaseJob("cavemanMerge", "merge", 1);
    cavemanMergeJob.setMaxMemory(memCavemanMerge);
    for(Job cavemanMstepJob : cavemanMstepJobs) {
      cavemanMergeJob.addParent(cavemanMstepJob);
    }
    
    List<Job> cavemanEstepJobs = new ArrayList<Job>();
    for(int i=0; i<coresAddressable; i++) {
      Job cavemanEstepJob = cavemanBaseJob("cavemanEstep", "estep", i+1);
      cavemanEstepJob.setMaxMemory(memCavemanEstep);
      cavemanEstepJob.addParent(cavemanMergeJob);
      cavemanEstepJobs.add(cavemanEstepJob);
    }
    
    Job cavemanMergeResultsJob = cavemanBaseJob("cavemanMergeResults", "merge_results", 1);
    cavemanMergeResultsJob.setMaxMemory(memCavemanMergeResults);
    for(Job cavemanEstepJob : cavemanEstepJobs) {
      cavemanMergeResultsJob.addParent(cavemanEstepJob);
    }
    
    Job cavemanAddIdsJob = cavemanBaseJob("cavemanAddIds", "add_ids", 1);
    cavemanAddIdsJob.setMaxMemory(memCavemanAddIds);
    cavemanAddIdsJob.addParent(cavemanMergeResultsJob);
    
    Job cavemanFlagJob = cavemanBaseJob("cavemanFlag", "flag", 1);
    cavemanFlagJob.setMaxMemory(memCavemanFlag);
    cavemanFlagJob.addParent(pindelFlagJob); // PINDEL dependency
    cavemanFlagJob.addParent(cavemanAddIdsJob);
    
    Job cavemanTbiCleanJob = cavemanTbiCleanJob();
    cavemanTbiCleanJob.setMaxMemory(memCavemanTbiClean);
    cavemanTbiCleanJob.addParent(cavemanFlagJob);
    
    Job cavemanPackage = packageResults("caveman", "snv_mnv", tumourBam, "flagged.muts.vcf.gz");
    cavemanPackage.setMaxMemory(memPackageResults);
    cavemanPackage.addParent(cavemanFlagJob);

    // @TODO then we need to write back to GNOS

  }
  
  private Job packageResults(String algName, String resultType, String tumourBam, String baseVcf) {
    //#packageResults.pl outdir 0772aed3-4df7-403f-802a-808df2935cd1/c007f362d965b32174ec030825262714.bam outdir/caveman snv_mnv flagged.muts.vcf.gz
    Job thisJob = getWorkflow().createBashJob("packageResults");
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument(LOGDIR.concat("packageResults.").concat(algName).concat(".log"))
              .addArgument(getWorkflowBaseDir() + "/bin/packageResults.pl")
              .addArgument(OUTDIR)
              .addArgument(tumourBam)
              .addArgument(OUTDIR + "/" + algName)
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
            .addArgument(LOGDIR.concat("basFileGet.log"))
            .addArgument("xml_to_bas.pl")
            .addArgument("-d " + gnosServer + "/cghub/metadata/analysisFull/" + analysisId)
            .addArgument("-o " + sampleBam + ".bas")
            ;
    return thisJob;
  }
  
  private Job caveCnPrep(String type) {
    String cnPath;
    if(ascatCn == null) {
      cnPath = OUTDIR + "/ascat/*.copynumber.caveman.csv";
    }
    else {
      cnPath = ascatCn;
    }
    
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
      .addArgument("> " + OUTDIR + "/" + type + ".cn.bed")
      ;
    thisJob.setMaxMemory(memCaveCnPrep);
    return thisJob;
  }
  
  private Job cavemanTbiCleanJob() {
    Job thisJob = getWorkflow().createBashJob("CaveTbiClean");
    thisJob.getCommand().addArgument("rm -f unmatchedNormal.*.vcf.gz.tbi");
    return thisJob;
  }
  
  private Job cavemanBaseJob(String name, String process, int index) {
    String ascatContamFile;
    if(ascatContam == null) {
      ascatContamFile = OUTDIR + "/ascat/*.samplestatistics.csv";
    }
    else {
      ascatContamFile = ascatContam;
    }
    
    Job thisJob = getWorkflow().createBashJob(name);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument(LOGDIR.concat(name).concat(".").concat(Integer.toString(index)).concat(".log"))
              .addArgument("caveman.pl")
              .addArgument("-p " + process)
              .addArgument("-i " + index)
            
              .addArgument("-r " + genomeFaGz + ".fai")
              .addArgument("-ig " + refBase + "/caveman/ucscHiDepth_0.01_merge1000_no_exon.tsv")
              .addArgument("-b " + refBase + "/caveman/flagging")
              .addArgument("-u " + tabixSrvUri)
              .addArgument("-np " + seqType)
              .addArgument("-tp " + seqType)
              .addArgument("-sa " + assembly)
              .addArgument("-s " + species)
              .addArgument("-st " + seqProtocol)
            
              .addArgument("-o " + OUTDIR + "/caveman")
              .addArgument("-tc " + OUTDIR + "/tumour.cn.bed")
              .addArgument("-nc " + OUTDIR + "/normal.cn.bed")
              .addArgument("-k " + ascatContamFile)
              .addArgument("-tb " + tumourBam)
              .addArgument("-nb " + controlBam)
            ;
    if(name.equals("cavemanMstep") || name.equals("cavemanEstep")) {
      thisJob.getCommand().addArgument("-l " + coresAddressable);
    }
    else if(name.equals("cavemanFlag")) {
      if(pindelGermline == null) {
        thisJob.getCommand().addArgument("-in " + OUTDIR + "/pindel/*.germline.bed");
      }
      else {
        thisJob.getCommand().addArgument("-in " + pindelGermline);
      }
    }

    return thisJob;
  }

  private Job cgpAscatBaseJob(String name, String process, int index) {
    Job thisJob = getWorkflow().createBashJob(name);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument(LOGDIR.concat(name).concat(".").concat(Integer.toString(index)).concat(".log"))
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
              .addArgument("-o " + OUTDIR + "/ascat")
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

  private Job pindelBaseJob(String name, String process, int index) {
    Job thisJob = getWorkflow().createBashJob(name);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument(LOGDIR.concat(name).concat(".").concat(Integer.toString(index)).concat(".log"))
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
              .addArgument("-o " + OUTDIR + "/pindel")
              .addArgument("-t " + tumourBam)
              .addArgument("-n " + controlBam)
              ;
    if(name.equals("pindelInput")) {
      thisJob.getCommand().addArgument("-c " + pindelInputThreads);
      thisJob.setThreads(pindelInputThreads);
    }
    return thisJob;
  }

  private Job brassBaseJob(String name, String process, int index) {
    Job thisJob = getWorkflow().createBashJob(name);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument(LOGDIR.concat(name).concat(".").concat(Integer.toString(index)).concat(".log"))
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
              .addArgument("-o " + OUTDIR + "/brass")
              .addArgument("-t " + tumourBam)
              .addArgument("-n " + controlBam)
            ;
    if(name.equals("brassFilter")) {
      String cnPath;
      if(ascatCn == null) {
        cnPath = OUTDIR + "/ascat/*.copynumber.caveman.csv";
      }
      else {
        cnPath = ascatCn;
      }
      thisJob.getCommand().addArgument("-a " + cnPath);
    }
    else if(name.endsWith("brassAssemble")) {
      thisJob.getCommand().addArgument("-l 1"); // regardless of number of splits, run sequentially
    }
    return thisJob;
  }

}
