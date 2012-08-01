package com.intellij.lang.javascript.flex.flashbuilder;

import com.intellij.lang.javascript.flex.FlexBundle;
import com.intellij.lang.javascript.flex.FlexUtils;
import com.intellij.lang.javascript.flex.projectStructure.model.OutputType;
import com.intellij.lang.javascript.flex.projectStructure.model.TargetPlatform;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import gnu.trove.THashMap;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class FlashBuilderProjectLoadUtil {

  private static final String PROJECT_NAME_TAG = "<projectDescription><name>";
  private static final String ACTION_SCRIPT_PROPERTIES_TAG = "actionScriptProperties";
  private static final String FXP_PROPERTIES_TAG = "fxpProperties";
  private static final String COMPILER_TAG = "compiler";
  private static final String SWC_TAG = "swc";
  private static final String SOURCE_FOLDER_PATH_ATTR = "sourceFolderPath";
  private static final String COMPILER_SOURCE_PATH_TAG = "compilerSourcePath";
  private static final String LINKED_TAG = "linked";
  private static final String COMPILER_SOURCE_PATH_ENTRY_TAG = "compilerSourcePathEntry";
  private static final String PATH_ATTR = "path";
  private static final String LOCATION_ATTR = "location";
  private static final String OUTPUT_FOLDER_LOCATION_ATTR = "outputFolderLocation";
  private static final String OUTPUT_FOLDER_PATH_ATTR = "outputFolderPath";
  private static final String MAIN_APP_PATH_ATTR = "mainApplicationPath";
  private static final String TARGET_PLAYER_VERSION_ATTR = "targetPlayerVersion";
  private static final String ADDITIONAL_COMPILER_ARGUMENTS_ATTR = "additionalCompilerArguments";
  private static final String HTML_GENERATE_ATTR = "htmlGenerate";
  private static final String LIBRARY_PATH_TAG = "libraryPath";
  private static final String LIBRARY_PATH_ENTRY_TAG = "libraryPathEntry";
  private static final String LIBRARY_KIND_ATTR = "kind";
  private static final String SWC_FOLDER_KIND = "1";
  private static final String SWC_FILE_KIND = "3";
  private static final String USE_SDK_KIND = "4";
  private static final String ANE_KIND = "5";
  private static final String FLEX_SDK_ATTR = "flexSDK";
  private static final String USE_APOLLO_CONFIG_ATTR = "useApolloConfig";
  private static final String PROJECT_DESCRIPTION_TAG = "projectDescription";
  private static final String NAME_TAG = "name";
  private static final String LINKED_RESOURCES_TAG = "linkedResources";
  private static final String LINK_TAG = "link";
  private static final String LOCATION_TAG = "location";
  private static final String FLEX_LIB_PROPERTIES_TAG = "flexLibProperties";
  private static final String NAMESPACE_MANIFESTS_TAG = "namespaceManifests";
  private static final String NAMESPACE_MANIFEST_ENTRY_TAG = "namespaceManifestEntry";
  private static final String MANIFEST_ATTR = "manifest";
  private static final String NAMESPACE_ATTR = "namespace";
  private static final String APPLICATIONS_ELEMENT = "applications";
  private static final String APPLICATION_ELEMENT = "application";
  private static final String MODULES_ELEMENT = "modules";
  private static final String MODULE_ELEMENT = "module";
  private static final String DEST_PATH_ATTR = "destPath";
  private static final String OPTIMIZE_ATTR = "optimize";
  private static final String APPLICATION_ATTR = "application";
  private static final String SOURCE_PATH_ATTR = "sourcePath";
  private static final String BUILD_CSS_FILES_ELEMENT = "buildCSSFiles";
  private static final String BUILD_CSS_FILE_ENTRY_ELEMENT = "buildCSSFileEntry";
  private static final String BUILD_TARGETS_ELEMENT = "buildTargets";
  private static final String BUILD_TARGET_ELEMENT = "buildTarget";
  private static final String BUILD_TARGET_NAME_ATTR = "buildTargetName";
  private static final String MULTI_PLATFORM_SETTINGS_ELEMENT = "multiPlatformSettings";
  private static final String ENABLED_ATTR = "enabled";
  private static final String ANDROID_PLATFORM_ATTR_VALUE = "com.adobe.flexide.multiplatform.android.platform";
  private static final String IOS_PLATFORM_ATTR_VALUE = "com.adobe.flexide.multiplatform.ios.platform";
  private static final String BLACKBERRY_PLATFORM_ATTR_VALUE = "com.qnx.flexide.multiplatform.qnx.platform";
  private static final String USE_MULTIPLATFORM_CONFIG_ATTR = "useMultiPlatformConfig";
  private static final String PROVISIONING_FILE_ATTR = "provisioningFile";
  private static final String AIR_SETTINGS_ELEMENT = "airSettings";
  private static final String AIR_CERTIFICATE_ATTR = "airCertificatePath";
  private static final String INCLUDE_RESOURCES_ELEMENT = "includeResources";
  private static final String RESOURCE_ENTRY_ELEMENT = "resourceEntry";
  private static final String DEFAULT_VALUE = "default";

  private FlashBuilderProjectLoadUtil() {
  }

  public static FlashBuilderProject getDummyFBProject(final String name) {
    assert ApplicationManager.getApplication().isUnitTestMode();
    final FlashBuilderProject fbProject = new FlashBuilderProject();
    fbProject.setName(name);
    fbProject.setOutputType(OutputType.Library);
    return fbProject;
  }

  @NotNull
  static String readProjectName(final String dotProjectFilePath) {
    FileInputStream fis = null;
    try {
      //noinspection IOResourceOpenedButNotSafelyClosed
      fis = new FileInputStream(dotProjectFilePath);
      final String name = FlexUtils.findXMLElement(fis, PROJECT_NAME_TAG);
      if (!StringUtil.isEmptyOrSpaces(name)) {
        //noinspection ConstantConditions
        return name;
      }
    }
    catch (IOException e) {/*ignore*/}
    finally {
      if (fis != null) {
        try {
          fis.close();
        }
        catch (IOException e) {/*ignore*/}
      }
    }
    return PathUtil.getFileName(PathUtil.getParentPath(dotProjectFilePath));
  }

  public static List<FlashBuilderProject> loadProjects(final Collection<String> dotProjectFilePaths, final boolean isArchive) {
    final List<FlashBuilderProject> flashBuilderProjects = new ArrayList<FlashBuilderProject>(dotProjectFilePaths.size());
    for (final String dotProjectFilePath : dotProjectFilePaths) {
      final VirtualFile dotProjectFile = LocalFileSystem.getInstance().findFileByPath(dotProjectFilePath);
      if (dotProjectFile != null) {
        flashBuilderProjects.add(loadProject(dotProjectFile, isArchive));
      }
    }
    return flashBuilderProjects;
  }

  public static FlashBuilderProject loadProject(final VirtualFile dotProjectFile, final boolean isArchive) {
    final FlashBuilderProject project = new FlashBuilderProject();

    loadProjectNameAndLinkedResources(project, dotProjectFile);
    loadOutputType(project, dotProjectFile);
    loadProjectRoot(project, dotProjectFile);

    final Map<String, String> pathReplacementMap =
      isArchive ? loadMapFromDotFxpPropertiesFile(dotProjectFile) : Collections.<String, String>emptyMap();
    loadInfoFromDotActionScriptPropertiesFile(project, dotProjectFile, pathReplacementMap);
    loadInfoFromDotFlexLibPropertiesFile(project, dotProjectFile);

    return project;
  }

  private static void loadProjectNameAndLinkedResources(final FlashBuilderProject project, final VirtualFile dotProjectFile) {
    try {
      final Document dotProjectDocument = JDOMUtil.loadDocument(dotProjectFile.getInputStream());
      final Element projectDescription = dotProjectDocument.getRootElement();
      if (projectDescription == null || !PROJECT_DESCRIPTION_TAG.equals(projectDescription.getName())) return;

      final String projectName = projectDescription.getChildText(NAME_TAG, projectDescription.getNamespace());
      project.setName(StringUtil.notNullize(projectName, FlexBundle.message("unnamed")));

      //noinspection unchecked
      for (final Element linkedResourcesElement : (Iterable<Element>)projectDescription
        .getChildren(LINKED_RESOURCES_TAG, projectDescription.getNamespace())) {
        //noinspection unchecked
        for (final Element linkElement : (Iterable<Element>)linkedResourcesElement
          .getChildren(LINK_TAG, linkedResourcesElement.getNamespace())) {

          final String linkName = linkElement.getChildText(NAME_TAG, linkElement.getNamespace());
          final String linkLocation = linkElement.getChildText(LOCATION_TAG, linkElement.getNamespace());

          if (!StringUtil.isEmptyOrSpaces(linkName) && !StringUtil.isEmptyOrSpaces(linkLocation)) {
            project.addLinkedResource(linkName, FileUtil.toSystemIndependentName(linkLocation));
          }
        }
      }
    }
    catch (JDOMException e) {/*ignore*/}
    catch (IOException e) {/*ignore*/}
  }

  private static Map<String, String> loadMapFromDotFxpPropertiesFile(final VirtualFile dotProjectFile) {
    final Map<String, String> result = new THashMap<String, String>();

    final VirtualFile dir = dotProjectFile.getParent();
    assert dir != null;
    final VirtualFile dotFxpPropertiesFile = dir.findChild(FlashBuilderImporter.DOT_FXP_PROPERTIES);
    if (dotFxpPropertiesFile != null) {
      try {
        final Document document = JDOMUtil.loadDocument(dotFxpPropertiesFile.getInputStream());
        final Element fxpPropertiesElement = document.getRootElement();
        if (fxpPropertiesElement == null || !FXP_PROPERTIES_TAG.equals(fxpPropertiesElement.getName())) return Collections.emptyMap();

        final Element swcElement = fxpPropertiesElement.getChild(SWC_TAG);
        if (swcElement != null) {
          //noinspection unchecked
          for (final Element linkedElement : ((Iterable<Element>)swcElement.getChildren(LINKED_TAG))) {
            final String location = linkedElement.getAttributeValue(LOCATION_ATTR);
            final String path = linkedElement.getAttributeValue(PATH_ATTR);
            if (!StringUtil.isEmptyOrSpaces(location) && !StringUtil.isEmptyOrSpaces(path)) {
              result.put(location, path);
            }
          }
        }
      }
      catch (JDOMException e) {/*ignore*/}
      catch (IOException e) {/*ignore*/}
    }

    return result;
  }

  private static void loadInfoFromDotActionScriptPropertiesFile(final FlashBuilderProject project,
                                                                final VirtualFile dotProjectFile,
                                                                final Map<String, String> pathReplacementMap) {
    final VirtualFile dir = dotProjectFile.getParent();
    assert dir != null;
    final VirtualFile dotActionScriptPropertiesFile = dir.findChild(FlashBuilderImporter.DOT_ACTION_SCRIPT_PROPERTIES);
    if (dotActionScriptPropertiesFile != null) {
      try {
        final Document dotActionScriptPropertiesDocument = JDOMUtil.loadDocument(dotActionScriptPropertiesFile.getInputStream());
        final Element actionScriptPropertiesElement = dotActionScriptPropertiesDocument.getRootElement();
        if (actionScriptPropertiesElement == null || !ACTION_SCRIPT_PROPERTIES_TAG.equals(actionScriptPropertiesElement.getName())) return;
        loadMainClassName(project, actionScriptPropertiesElement);

        final Element compilerElement = actionScriptPropertiesElement.getChild(COMPILER_TAG);
        if (compilerElement != null) {
          loadProjectType(project, dotProjectFile, compilerElement);
          loadSourcePaths(project, compilerElement);
          loadOutputFolderPath(project, compilerElement);
          loadTargetPlayerVersion(project, compilerElement);
          loadAdditionalCompilerArguments(project, compilerElement);
          project.setUseHtmlWrapper("true".equals(compilerElement.getAttributeValue(HTML_GENERATE_ATTR)));
          loadDependenciesAndCheckIfSdkUsed(project, compilerElement, pathReplacementMap);
          if (project.isSdkUsed()) {
            loadSdkName(project, compilerElement);
          }
        }

        if (project.getOutputType() == OutputType.Application) {
          loadApplications(project, actionScriptPropertiesElement);
          loadModules(project, actionScriptPropertiesElement);
          loadCssFilesToCompile(project, actionScriptPropertiesElement);
        }
      }
      catch (JDOMException e) {/*ignore*/}
      catch (IOException e) {/*ignore*/}
    }
  }

  private static void loadInfoFromDotFlexLibPropertiesFile(final FlashBuilderProject project, final VirtualFile dotProjectFile) {
    final VirtualFile dotFlexLibPropertiesFile = dotProjectFile.getParent().findChild(FlashBuilderImporter.DOT_FLEX_LIB_PROPERTIES);
    if (dotFlexLibPropertiesFile != null) {
      try {
        final Document dotFlexLibPropertiesDocument = JDOMUtil.loadDocument(dotFlexLibPropertiesFile.getInputStream());
        final Element flexLibPropertiesElement = dotFlexLibPropertiesDocument.getRootElement();
        if (flexLibPropertiesElement == null || !FLEX_LIB_PROPERTIES_TAG.equals(flexLibPropertiesElement.getName())) return;

        if (project.getTargetPlatform() == TargetPlatform.Desktop &&
            "true".equals(flexLibPropertiesElement.getAttributeValue(USE_MULTIPLATFORM_CONFIG_ATTR))) {
          project.setTargetPlatform(TargetPlatform.Mobile);
        }

        //noinspection unchecked
        for (final Element namespaceManifestsElement : (Iterable<Element>)flexLibPropertiesElement
          .getChildren(NAMESPACE_MANIFESTS_TAG, flexLibPropertiesElement.getNamespace())) {
          //noinspection unchecked
          for (final Element namespaceManifestEntryElement : (Iterable<Element>)namespaceManifestsElement
            .getChildren(NAMESPACE_MANIFEST_ENTRY_TAG, namespaceManifestsElement.getNamespace())) {
            final String namespace = namespaceManifestEntryElement.getAttributeValue(NAMESPACE_ATTR);
            final String manifestPath = namespaceManifestEntryElement.getAttributeValue(MANIFEST_ATTR);
            if (!StringUtil.isEmpty(manifestPath) && !StringUtil.isEmpty(namespace)) {
              project.addNamespaceAndManifestPath(namespace, FileUtil.toSystemIndependentName(manifestPath));
            }
          }
        }
      }
      catch (JDOMException e) {/*ignore*/}
      catch (IOException e) {/*ignore*/}
    }
  }

  private static void loadProjectType(final FlashBuilderProject flashBuilderProject,
                                      final VirtualFile dotProjectFile,
                                      final Element compilerElement) {
    final VirtualFile dir = dotProjectFile.getParent();
    assert dir != null;

    final VirtualFile flexLibPropertiesFile = dir.findChild(FlashBuilderImporter.DOT_FLEX_LIB_PROPERTIES);
    flashBuilderProject.setPureActionScript(dir.findChild(FlashBuilderImporter.DOT_FLEX_PROPERTIES) == null &&
                                            flexLibPropertiesFile == null);
    if (flexLibPropertiesFile == null) {
      final Element parentElement = compilerElement.getParentElement();
      //noinspection unchecked
      for (final Element buildTargetsElement : (Iterable<Element>)(parentElement
                                                                     .getChildren(BUILD_TARGETS_ELEMENT, parentElement.getNamespace()))) {
        //noinspection unchecked
        for (final Element buildTargetElement : (Iterable<Element>)(buildTargetsElement
                                                                      .getChildren(BUILD_TARGET_ELEMENT, parentElement.getNamespace()))) {
          final String buildTarget = buildTargetElement.getAttributeValue(BUILD_TARGET_NAME_ATTR);
          if (ANDROID_PLATFORM_ATTR_VALUE.equals(buildTarget)) {
            flashBuilderProject.setAndroidSupported(isPlatformEnabled(buildTargetElement));
            flashBuilderProject.setTargetPlatform(TargetPlatform.Mobile);
            loadSigningOptions(flashBuilderProject, buildTargetElement, TargetPlatform.Mobile, false);
          }
          else if (IOS_PLATFORM_ATTR_VALUE.equals(buildTarget)) {
            flashBuilderProject.setIosSupported(isPlatformEnabled(buildTargetElement));
            flashBuilderProject.setTargetPlatform(TargetPlatform.Mobile);
            loadSigningOptions(flashBuilderProject, buildTargetElement, TargetPlatform.Mobile, true);
          }
          else if (BLACKBERRY_PLATFORM_ATTR_VALUE.equals(buildTarget)) {
            flashBuilderProject.setTargetPlatform(TargetPlatform.Mobile);
          }
          else if (DEFAULT_VALUE.equals(buildTarget)) {
            loadSigningOptions(flashBuilderProject, buildTargetElement, TargetPlatform.Desktop, false);
          }
        }
      }
    }
    else {
      // if this is Pure AS Mobile library its target platform will be read a bit later in loadInfoFromDotFlexLibPropertiesFile()}
    }

    if (flashBuilderProject.getTargetPlatform() == TargetPlatform.Mobile) {
      return;
    }

    if ("true".equals(compilerElement.getAttributeValue(USE_APOLLO_CONFIG_ATTR))) {
      flashBuilderProject.setTargetPlatform(TargetPlatform.Desktop);
    }
    else {
      flashBuilderProject.setTargetPlatform(TargetPlatform.Web);
    }
  }

  private static boolean isPlatformEnabled(final Element buildTargetElement) {
    final Element multiPlatformSettings = buildTargetElement.getChild(MULTI_PLATFORM_SETTINGS_ELEMENT, buildTargetElement.getNamespace());
    return multiPlatformSettings != null && "true".equals(multiPlatformSettings.getAttributeValue(ENABLED_ATTR));
  }

  private static void loadSigningOptions(final FlashBuilderProject fbProject,
                                         final Element buildTargetElement,
                                         final TargetPlatform targetPlatform,
                                         final boolean iOS) {
    final Element airSettingsElement = buildTargetElement.getChild(AIR_SETTINGS_ELEMENT, buildTargetElement.getNamespace());
    if (airSettingsElement == null) return;

    final String certPath = airSettingsElement.getAttributeValue(AIR_CERTIFICATE_ATTR);
    if (certPath != null) {
      if (targetPlatform == TargetPlatform.Desktop) {
        fbProject.setDesktopCertPath(FileUtil.toSystemIndependentName(certPath));
      }
      else {
        if (iOS) {
          fbProject.setIOSCertPath(FileUtil.toSystemIndependentName(certPath));
        }
        else {
          fbProject.setAndroidCertPath(FileUtil.toSystemIndependentName(certPath));
        }
      }
    }

    if (targetPlatform == TargetPlatform.Mobile && iOS) {
      final String provisioningPath = buildTargetElement.getAttributeValue(PROVISIONING_FILE_ATTR);
      if (provisioningPath != null) {
        fbProject.setIOSProvisioningPath(provisioningPath);
      }
    }
  }

  private static void loadOutputType(final FlashBuilderProject project, final VirtualFile dotProjectFile) {
    final VirtualFile dir = dotProjectFile.getParent();
    assert dir != null;
    project.setOutputType(dir.findChild(FlashBuilderImporter.DOT_FLEX_LIB_PROPERTIES) == null
                          ? OutputType.Application
                          : OutputType.Library);
  }

  private static void loadProjectRoot(final FlashBuilderProject project, final VirtualFile dotProjectFile) {
    final VirtualFile dir = dotProjectFile.getParent();
    assert dir != null;
    project.setProjectRootPath(dir.getPath());
  }

  private static void loadSourcePaths(final FlashBuilderProject project, final Element compilerElement) {
    final String sourceFolderPath = compilerElement.getAttributeValue(SOURCE_FOLDER_PATH_ATTR);
    if (!StringUtil.isEmptyOrSpaces(sourceFolderPath)) {
      project.addSourcePath(FileUtil.toSystemIndependentName(sourceFolderPath));
    }
    //noinspection unchecked
    for (final Element compilerSourcePathElement : ((Iterable<Element>)compilerElement.getChildren(COMPILER_SOURCE_PATH_TAG))) {
      //noinspection unchecked
      for (final Element compilerSourcePathEntryElement : ((Iterable<Element>)compilerSourcePathElement
        .getChildren(COMPILER_SOURCE_PATH_ENTRY_TAG))) {
        final String sourcePath = compilerSourcePathEntryElement.getAttributeValue(PATH_ATTR);
        if (!StringUtil.isEmptyOrSpaces(sourcePath)) {
          project.addSourcePath(FileUtil.toSystemIndependentName(sourcePath));
        }
      }
    }
  }

  private static void loadOutputFolderPath(final FlashBuilderProject project, final Element compilerElement) {
    final String outputFolderLocation = compilerElement.getAttributeValue(OUTPUT_FOLDER_LOCATION_ATTR);
    if (!StringUtil.isEmptyOrSpaces(outputFolderLocation)) {
      project.setOutputFolderPath(FileUtil.toSystemIndependentName(outputFolderLocation));
    }
    else {
      final String outputFolderPath = compilerElement.getAttributeValue(OUTPUT_FOLDER_PATH_ATTR);
      if (!StringUtil.isEmptyOrSpaces(outputFolderPath)) {
        project.setOutputFolderPath(FileUtil.toSystemIndependentName(outputFolderPath));
      }
    }
  }

  private static void loadMainClassName(final FlashBuilderProject project, final Element actionScriptPropertiesElement) {
    final String mainAppPath = actionScriptPropertiesElement.getAttributeValue(MAIN_APP_PATH_ATTR);
    if (mainAppPath != null) {
      project.setMainAppClassName(getClassName(mainAppPath));
    }
  }

  public static String getClassName(final @NotNull String path) {
    final String fqn = path.replace('/', '.').trim();
    final int lastDotIndex = fqn.lastIndexOf('.');
    final String lowercased = fqn.toLowerCase();
    return (lastDotIndex >= 0 && (lowercased.endsWith(".mxml") || lowercased.endsWith(".as"))) ? fqn.substring(0, lastDotIndex) : fqn;
  }

  private static void loadTargetPlayerVersion(final FlashBuilderProject project, final Element compilerElement) {
    final Attribute targetPlayerVersionAttr = compilerElement.getAttribute(TARGET_PLAYER_VERSION_ATTR);
    if (targetPlayerVersionAttr != null) {
      final String version = targetPlayerVersionAttr.getValue();
      if (!version.startsWith("0")) {
        project.setTargetPlayerVersion(version);
      }
    }
  }

  private static void loadAdditionalCompilerArguments(final FlashBuilderProject project, final Element compilerElement) {
    final Attribute additionalCompilerArgumentsAttr = compilerElement.getAttribute(ADDITIONAL_COMPILER_ARGUMENTS_ATTR);
    if (additionalCompilerArgumentsAttr != null) {
      project.setAdditionalCompilerOptions(additionalCompilerArgumentsAttr.getValue());
    }
  }

  private static void loadDependenciesAndCheckIfSdkUsed(final FlashBuilderProject project,
                                                        final Element compilerElement,
                                                        final Map<String, String> pathReplacementMap) {
    //noinspection unchecked
    for (final Element libraryPathElement : ((Iterable<Element>)compilerElement.getChildren(LIBRARY_PATH_TAG))) {
      //noinspection unchecked
      for (final Element libraryPathEntryElement : ((Iterable<Element>)libraryPathElement.getChildren(LIBRARY_PATH_ENTRY_TAG))) {
        final Attribute libraryKindAttr = libraryPathEntryElement.getAttribute(LIBRARY_KIND_ATTR);
        final String libraryKind = libraryKindAttr != null ? libraryKindAttr.getValue() : SWC_FILE_KIND;
        if (libraryKind.equals(USE_SDK_KIND)) {
          project.setSdkUsed(true);
        }
        else {
          final String libraryPath = libraryPathEntryElement.getAttributeValue(PATH_ATTR);
          if (!StringUtil.isEmptyOrSpaces(libraryPath)) {
            if (SWC_FILE_KIND.equals(libraryKind) || SWC_FOLDER_KIND.equals(libraryKind) || ANE_KIND.equals(libraryKind)) {
              // TODO: parse sources
              final Collection<String> librarySourcePaths = new ArrayList<String>();

              final String replacedPath = pathReplacementMap.get(libraryPath);
              project.addLibraryPathAndSources(FileUtil.toSystemIndependentName(replacedPath != null ? replacedPath : libraryPath),
                                               librarySourcePaths);
            }
          }
        }
      }
    }
  }

  private static void loadSdkName(final FlashBuilderProject project, final Element compilerElement) {
    final Attribute flexSdkAttr = compilerElement.getAttribute(FLEX_SDK_ATTR);
    if (flexSdkAttr != null) {
      project.setSdkName(flexSdkAttr.getValue());
    }
  }

  private static void loadApplications(final FlashBuilderProject project, final Element actionScriptPropertiesElement) {
    //noinspection unchecked
    for (final Element applicationsElement : ((Iterable<Element>)actionScriptPropertiesElement.getChildren(APPLICATIONS_ELEMENT))) {
      //noinspection unchecked
      for (final Element applicationElement : ((Iterable<Element>)applicationsElement.getChildren(APPLICATION_ELEMENT))) {
        final String path = applicationElement.getAttributeValue(PATH_ATTR);
        if (path != null) {
          project.addApplicationClassName(getClassName(path));
        }
      }
    }
  }

  private static void loadModules(final FlashBuilderProject project, final Element actionScriptPropertiesElement) {
    //noinspection unchecked
    for (final Element modulesElement : ((Iterable<Element>)actionScriptPropertiesElement.getChildren(MODULES_ELEMENT))) {
      //noinspection unchecked
      for (final Element moduleElement : ((Iterable<Element>)modulesElement.getChildren(MODULE_ELEMENT))) {
        final String mainClassPath = moduleElement.getAttributeValue(SOURCE_PATH_ATTR);
        final String outputPath = moduleElement.getAttributeValue(DEST_PATH_ATTR);
        final String optimize = moduleElement.getAttributeValue(OPTIMIZE_ATTR);
        final String optimizeFor = moduleElement.getAttributeValue(APPLICATION_ATTR);
        if (!StringUtil.isEmpty(mainClassPath) && !StringUtil.isEmpty(DEST_PATH_ATTR) && !StringUtil.isEmpty(optimizeFor)) {
          project.addModule(new FlashBuilderProject.FBRLMInfo(mainClassPath, outputPath, "true".equalsIgnoreCase(optimize), optimizeFor));
        }
      }
    }
  }

  private static void loadCssFilesToCompile(final FlashBuilderProject project, final Element actionScriptPropertiesElement) {
    //noinspection unchecked
    for (final Element buildSccFilesElement : ((Iterable<Element>)actionScriptPropertiesElement.getChildren(BUILD_CSS_FILES_ELEMENT))) {
      //noinspection unchecked
      for (final Element buildCssFileEntryElement : ((Iterable<Element>)buildSccFilesElement.getChildren(BUILD_CSS_FILE_ENTRY_ELEMENT))) {
        final String sourcePath = buildCssFileEntryElement.getAttributeValue(SOURCE_PATH_ATTR);
        if (!StringUtil.isEmpty(sourcePath)) {
          project.addCssFileToCompile(FileUtil.toSystemIndependentName(sourcePath));
        }
      }
    }
  }
}
