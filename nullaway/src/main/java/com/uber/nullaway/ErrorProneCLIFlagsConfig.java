/*
 * Copyright (c) 2017 Uber Technologies, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.uber.nullaway;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.ErrorProneFlags;
import com.uber.nullaway.fixserialization.FixSerializationConfig;
import com.uber.nullaway.fixserialization.adapters.SerializationAdapter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * provides nullability configuration based on additional flags passed to ErrorProne via
 * "-XepOpt:[Namespace:]FlagName[=Value]". See. http://errorprone.info/docs/flags
 */
final class ErrorProneCLIFlagsConfig extends AbstractConfig {

  private static final String BASENAME_REGEX = ".*/([^/]+)\\.[ja]ar$";

  static final String EP_FL_NAMESPACE = "NullAway";
  static final String FL_ANNOTATED_PACKAGES = EP_FL_NAMESPACE + ":AnnotatedPackages";
  static final String FL_ASSERTS_ENABLED = EP_FL_NAMESPACE + ":AssertsEnabled";
  static final String FL_UNANNOTATED_SUBPACKAGES = EP_FL_NAMESPACE + ":UnannotatedSubPackages";
  static final String FL_CLASSES_TO_EXCLUDE = EP_FL_NAMESPACE + ":ExcludedClasses";
  static final String FL_EXHAUSTIVE_OVERRIDE = EP_FL_NAMESPACE + ":ExhaustiveOverride";
  static final String FL_KNOWN_INITIALIZERS = EP_FL_NAMESPACE + ":KnownInitializers";
  static final String FL_CLASS_ANNOTATIONS_TO_EXCLUDE =
      EP_FL_NAMESPACE + ":ExcludedClassAnnotations";
  static final String FL_SUGGEST_SUPPRESSIONS = EP_FL_NAMESPACE + ":SuggestSuppressions";

  static final String FL_CLASS_ANNOTATIONS_GENERATED =
      EP_FL_NAMESPACE + ":CustomGeneratedCodeAnnotations";
  static final String FL_GENERATED_UNANNOTATED = EP_FL_NAMESPACE + ":TreatGeneratedAsUnannotated";
  static final String FL_ACKNOWLEDGE_ANDROID_RECENT = EP_FL_NAMESPACE + ":AcknowledgeAndroidRecent";
  static final String FL_JSPECIFY_MODE = EP_FL_NAMESPACE + ":JSpecifyMode";
  static final String FL_EXCLUDED_FIELD_ANNOT = EP_FL_NAMESPACE + ":ExcludedFieldAnnotations";
  static final String FL_INITIALIZER_ANNOT = EP_FL_NAMESPACE + ":CustomInitializerAnnotations";
  static final String FL_NULLABLE_ANNOT = EP_FL_NAMESPACE + ":CustomNullableAnnotations";
  static final String FL_NONNULL_ANNOT = EP_FL_NAMESPACE + ":CustomNonnullAnnotations";
  static final String FL_CTNN_METHOD = EP_FL_NAMESPACE + ":CastToNonNullMethod";
  static final String FL_EXTERNAL_INIT_ANNOT = EP_FL_NAMESPACE + ":ExternalInitAnnotations";
  static final String FL_CONTRACT_ANNOT = EP_FL_NAMESPACE + ":CustomContractAnnotations";
  static final String FL_UNANNOTATED_CLASSES = EP_FL_NAMESPACE + ":UnannotatedClasses";
  static final String FL_ACKNOWLEDGE_RESTRICTIVE =
      EP_FL_NAMESPACE + ":AcknowledgeRestrictiveAnnotations";
  static final String FL_CHECK_OPTIONAL_EMPTINESS = EP_FL_NAMESPACE + ":CheckOptionalEmptiness";
  static final String FL_CHECK_CONTRACTS = EP_FL_NAMESPACE + ":CheckContracts";
  static final String FL_HANDLE_TEST_ASSERTION_LIBRARIES =
      EP_FL_NAMESPACE + ":HandleTestAssertionLibraries";
  static final String FL_OPTIONAL_CLASS_PATHS =
      EP_FL_NAMESPACE + ":CheckOptionalEmptinessCustomClasses";
  static final String FL_SUPPRESS_COMMENT = EP_FL_NAMESPACE + ":AutoFixSuppressionComment";

  static final String FL_SKIP_LIBRARY_MODELS = EP_FL_NAMESPACE + ":IgnoreLibraryModelsFor";

  /** --- JarInfer configs --- */
  static final String FL_JI_ENABLED = EP_FL_NAMESPACE + ":JarInferEnabled";

  static final String FL_JI_USE_RETURN = EP_FL_NAMESPACE + ":JarInferUseReturnAnnotations";

  static final String FL_JI_REGEX_MODEL_PATH = EP_FL_NAMESPACE + ":JarInferRegexStripModelJar";
  static final String FL_JI_REGEX_CODE_PATH = EP_FL_NAMESPACE + ":JarInferRegexStripCodeJar";
  static final String FL_ERROR_URL = EP_FL_NAMESPACE + ":ErrorURL";

  /** --- Serialization configs --- */
  static final String FL_FIX_SERIALIZATION = EP_FL_NAMESPACE + ":SerializeFixMetadata";

  static final String FL_FIX_SERIALIZATION_VERSION =
      EP_FL_NAMESPACE + ":SerializeFixMetadataVersion";

  static final String FL_FIX_SERIALIZATION_CONFIG_PATH =
      EP_FL_NAMESPACE + ":FixSerializationConfigPath";

  private static final String DELIMITER = ",";

  static final ImmutableSet<String> DEFAULT_CLASS_ANNOTATIONS_TO_EXCLUDE =
      ImmutableSet.of("lombok.Generated");

  // Annotations with simple name ".Generated" need not be manually listed, and are always matched
  // by default
  // TODO: org.apache.avro.specific.AvroGenerated should go here, but we are skipping it for the
  // next release to better test the effect of this feature (users can always manually configure
  // it).
  static final ImmutableSet<String> DEFAULT_CLASS_ANNOTATIONS_GENERATED = ImmutableSet.of();

  static final ImmutableSet<String> DEFAULT_KNOWN_INITIALIZERS =
      ImmutableSet.of(
          "android.view.View.onFinishInflate",
          "android.app.Service.onCreate",
          "android.app.Activity.onCreate",
          "android.app.Fragment.onCreate",
          "android.app.Fragment.onAttach",
          "android.app.Fragment.onCreateView",
          "android.app.Fragment.onViewCreated",
          "android.app.Application.onCreate",
          "javax.annotation.processing.Processor.init",
          // Support Library v4 - can be removed once AndroidX becomes more popular
          "android.support.v4.app.ActivityCompat.onCreate",
          "android.support.v4.app.Fragment.onCreate",
          "android.support.v4.app.Fragment.onAttach",
          "android.support.v4.app.Fragment.onCreateView",
          "android.support.v4.app.Fragment.onViewCreated",
          // Support Library v4 - can be removed once AndroidX becomes more popular
          "androidx.core.app.ActivityCompat.onCreate",
          "androidx.fragment.app.Fragment.onCreate",
          "androidx.fragment.app.Fragment.onAttach",
          "androidx.fragment.app.Fragment.onCreateView",
          "androidx.fragment.app.Fragment.onActivityCreated",
          "androidx.fragment.app.Fragment.onViewCreated",
          // Multidex app
          "android.support.multidex.Application.onCreate");

  static final ImmutableSet<String> DEFAULT_INITIALIZER_ANNOT =
      ImmutableSet.of(
          "org.junit.Before",
          "org.junit.BeforeClass",
          "org.junit.jupiter.api.BeforeAll",
          "org.junit.jupiter.api.BeforeEach",
          "org.springframework.beans.factory.annotation.Autowired");
  // + Anything with @Initializer as its "simple name"

  static final ImmutableSet<String> DEFAULT_EXTERNAL_INIT_ANNOT = ImmutableSet.of("lombok.Builder");

  static final ImmutableSet<String> DEFAULT_CONTRACT_ANNOT =
      ImmutableSet.of("org.jetbrains.annotations.Contract");

  static final ImmutableSet<String> DEFAULT_EXCLUDED_FIELD_ANNOT =
      ImmutableSet.of(
          "jakarta.inject.Inject", // no explicit initialization when there is dependency injection
          "javax.inject.Inject", // no explicit initialization when there is dependency injection
          "com.google.errorprone.annotations.concurrent.LazyInit",
          "org.checkerframework.checker.nullness.qual.MonotonicNonNull",
          "org.springframework.beans.factory.annotation.Autowired");

  private static final String DEFAULT_URL = "http://t.uber.com/nullaway";

  ErrorProneCLIFlagsConfig(ErrorProneFlags flags) {
    if (!flags.get(FL_ANNOTATED_PACKAGES).isPresent()) {
      throw new IllegalStateException(
          "DO NOT report an issue to Error Prone for this crash!  NullAway configuration is "
              + "incorrect.  "
              + "Must specify annotated packages, using the "
              + "-XepOpt:"
              + FL_ANNOTATED_PACKAGES
              + "=[...] flag.  If you feel you have gotten this message in error report an issue"
              + " at https://github.com/uber/NullAway/issues.");
    }
    annotatedPackages = getPackagePattern(getFlagStringSet(flags, FL_ANNOTATED_PACKAGES));
    unannotatedSubPackages = getPackagePattern(getFlagStringSet(flags, FL_UNANNOTATED_SUBPACKAGES));
    sourceClassesToExclude = getFlagStringSet(flags, FL_CLASSES_TO_EXCLUDE);
    unannotatedClasses = getFlagStringSet(flags, FL_UNANNOTATED_CLASSES);
    knownInitializers =
        getFlagStringSet(flags, FL_KNOWN_INITIALIZERS, DEFAULT_KNOWN_INITIALIZERS).stream()
            .map(MethodClassAndName::fromClassDotMethod)
            .collect(ImmutableSet.toImmutableSet());
    excludedClassAnnotations =
        getFlagStringSet(
            flags, FL_CLASS_ANNOTATIONS_TO_EXCLUDE, DEFAULT_CLASS_ANNOTATIONS_TO_EXCLUDE);
    generatedCodeAnnotations =
        getFlagStringSet(
            flags, FL_CLASS_ANNOTATIONS_GENERATED, DEFAULT_CLASS_ANNOTATIONS_GENERATED);
    initializerAnnotations =
        getFlagStringSet(flags, FL_INITIALIZER_ANNOT, DEFAULT_INITIALIZER_ANNOT);
    customNullableAnnotations = getFlagStringSet(flags, FL_NULLABLE_ANNOT, ImmutableSet.of());
    customNonnullAnnotations = getFlagStringSet(flags, FL_NONNULL_ANNOT, ImmutableSet.of());
    externalInitAnnotations =
        getFlagStringSet(flags, FL_EXTERNAL_INIT_ANNOT, DEFAULT_EXTERNAL_INIT_ANNOT);
    contractAnnotations = getFlagStringSet(flags, FL_CONTRACT_ANNOT, DEFAULT_CONTRACT_ANNOT);
    isExhaustiveOverride = flags.getBoolean(FL_EXHAUSTIVE_OVERRIDE).orElse(false);
    isSuggestSuppressions = flags.getBoolean(FL_SUGGEST_SUPPRESSIONS).orElse(false);
    isAcknowledgeRestrictive = flags.getBoolean(FL_ACKNOWLEDGE_RESTRICTIVE).orElse(false);
    checkOptionalEmptiness = flags.getBoolean(FL_CHECK_OPTIONAL_EMPTINESS).orElse(false);
    checkContracts = flags.getBoolean(FL_CHECK_CONTRACTS).orElse(false);
    handleTestAssertionLibraries =
        flags.getBoolean(FL_HANDLE_TEST_ASSERTION_LIBRARIES).orElse(false);
    treatGeneratedAsUnannotated = flags.getBoolean(FL_GENERATED_UNANNOTATED).orElse(false);
    acknowledgeAndroidRecent = flags.getBoolean(FL_ACKNOWLEDGE_ANDROID_RECENT).orElse(false);
    jspecifyMode = flags.getBoolean(FL_JSPECIFY_MODE).orElse(false);
    assertsEnabled = flags.getBoolean(FL_ASSERTS_ENABLED).orElse(false);
    fieldAnnotPattern =
        getPackagePattern(
            getFlagStringSet(flags, FL_EXCLUDED_FIELD_ANNOT, DEFAULT_EXCLUDED_FIELD_ANNOT));
    castToNonNullMethod = flags.get(FL_CTNN_METHOD).orElse(null);
    autofixSuppressionComment = flags.get(FL_SUPPRESS_COMMENT).orElse("");
    optionalClassPaths =
        new ImmutableSet.Builder<String>()
            .addAll(getFlagStringSet(flags, FL_OPTIONAL_CLASS_PATHS))
            .add("java.util.Optional")
            .build();
    if (autofixSuppressionComment.contains("\n")) {
      throw new IllegalStateException(
          "Invalid -XepOpt:" + FL_SUPPRESS_COMMENT + " value. Comment must be single line.");
    }
    skippedLibraryModels = getFlagStringSet(flags, FL_SKIP_LIBRARY_MODELS);
    /* --- JarInfer configs --- */
    jarInferEnabled = flags.getBoolean(FL_JI_ENABLED).orElse(false);
    jarInferUseReturnAnnotations = flags.getBoolean(FL_JI_USE_RETURN).orElse(false);
    // The defaults of these two options translate to: remove .aar/.jar from the file name, and also
    // implicitly mean that NullAway will search for jarinfer models in the same jar which contains
    // the analyzed classes.
    jarInferRegexStripModelJarName = flags.get(FL_JI_REGEX_MODEL_PATH).orElse(BASENAME_REGEX);
    jarInferRegexStripCodeJarName = flags.get(FL_JI_REGEX_CODE_PATH).orElse(BASENAME_REGEX);
    errorURL = flags.get(FL_ERROR_URL).orElse(DEFAULT_URL);
    if (acknowledgeAndroidRecent && !isAcknowledgeRestrictive) {
      throw new IllegalStateException(
          "-XepOpt:"
              + FL_ACKNOWLEDGE_ANDROID_RECENT
              + " should only be set when -XepOpt:"
              + FL_ACKNOWLEDGE_RESTRICTIVE
              + " is also set");
    }
    serializationActivationFlag = flags.getBoolean(FL_FIX_SERIALIZATION).orElse(false);
    Optional<String> fixSerializationConfigPath = flags.get(FL_FIX_SERIALIZATION_CONFIG_PATH);
    if (serializationActivationFlag && !fixSerializationConfigPath.isPresent()) {
      throw new IllegalStateException(
          "DO NOT report an issue to Error Prone for this crash!  NullAway Fix Serialization configuration is "
              + "incorrect.  "
              + "Must specify AutoFixer Output Directory, using the "
              + "-XepOpt:"
              + FL_FIX_SERIALIZATION_CONFIG_PATH
              + " flag.  If you feel you have gotten this message in error report an issue"
              + " at https://github.com/uber/NullAway/issues.");
    }
    int serializationVersion =
        flags.getInteger(FL_FIX_SERIALIZATION_VERSION).orElse(SerializationAdapter.LATEST_VERSION);
    /*
     * if fixSerializationActivationFlag is false, the default constructor is invoked for
     * creating FixSerializationConfig which all features are deactivated.  This lets the
     * field be @Nonnull, allowing us to avoid null checks in various places.
     */
    fixSerializationConfig =
        serializationActivationFlag && fixSerializationConfigPath.isPresent()
            ? new FixSerializationConfig(fixSerializationConfigPath.get(), serializationVersion)
            : new FixSerializationConfig();
    if (serializationActivationFlag && isSuggestSuppressions) {
      throw new IllegalStateException(
          "In order to activate Fix Serialization mode ("
              + FL_FIX_SERIALIZATION
              + "), Suggest Suppressions mode must be deactivated ("
              + FL_SUGGEST_SUPPRESSIONS
              + ")");
    }
  }

  private static ImmutableSet<String> getFlagStringSet(ErrorProneFlags flags, String flagName) {
    Optional<String> flagValue = flags.get(flagName);
    if (flagValue.isPresent()) {
      return ImmutableSet.copyOf(flagValue.get().split(DELIMITER));
    }
    return ImmutableSet.of();
  }

  private static ImmutableSet<String> getFlagStringSet(
      ErrorProneFlags flags, String flagName, ImmutableSet<String> defaults) {
    Set<String> combined = new LinkedHashSet<>(defaults);
    Optional<String> flagValue = flags.get(flagName);
    if (flagValue.isPresent()) {
      Collections.addAll(combined, flagValue.get().split(DELIMITER));
    }
    return ImmutableSet.copyOf(combined);
  }
}

/*
 * Copyright (c) 2019 Uber Technologies, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.uber.nullaway;

import static com.uber.nullaway.ASTHelpersBackports.isStatic;
import static com.uber.nullaway.ErrorMessage.MessageTypes.FIELD_NO_INIT;
import static com.uber.nullaway.ErrorMessage.MessageTypes.GET_ON_EMPTY_OPTIONAL;
import static com.uber.nullaway.ErrorMessage.MessageTypes.METHOD_NO_INIT;
import static com.uber.nullaway.ErrorMessage.MessageTypes.NONNULL_FIELD_READ_BEFORE_INIT;
import static com.uber.nullaway.NullAway.CORE_CHECK_NAME;
import static com.uber.nullaway.NullAway.INITIALIZATION_CHECK_NAME;
import static com.uber.nullaway.NullAway.OPTIONAL_CHECK_NAME;
import static com.uber.nullaway.NullAway.getTreesInstance;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.uber.nullaway.fixserialization.SerializationService;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;

/** A class to construct error message to be displayed after the analysis finds error. */
public class ErrorBuilder {

  private final Config config;

  /** Checker name that can be used to suppress the warnings. */
  private final String suppressionName;

  /** Additional identifiers for this check, to be checked for in @SuppressWarnings annotations. */
  private final Set<String> allNames;

  ErrorBuilder(Config config, String suppressionName, Set<String> allNames) {
    this.config = config;
    this.suppressionName = suppressionName;
    this.allNames = allNames;
  }

  /**
   * create an error description for a nullability warning
   *
   * @param errorMessage the error message object.
   * @param descriptionBuilder the description builder for the error.
   * @param state the visitor state (used for e.g. suppression finding).
   * @param nonNullTarget if non-null, this error involved a pseudo-assignment of a @Nullable
   *     expression into a @NonNull target, and this parameter is the Symbol for that target.
   * @return the error description
   */
  Description createErrorDescription(
      ErrorMessage errorMessage,
      Description.Builder descriptionBuilder,
      VisitorState state,
      @Nullable Symbol nonNullTarget) {
    Tree enclosingSuppressTree = suppressibleNode(state.getPath());
    return createErrorDescription(
        errorMessage, enclosingSuppressTree, descriptionBuilder, state, nonNullTarget);
  }

  /**
   * create an error description for a nullability warning
   *
   * @param errorMessage the error message object.
   * @param suggestTree the location at which a fix suggestion should be made
   * @param descriptionBuilder the description builder for the error.
   * @param state the visitor state (used for e.g. suppression finding).
   * @param nonNullTarget if non-null, this error involved a pseudo-assignment of a @Nullable
   *     expression into a @NonNull target, and this parameter is the Symbol for that target.
   * @return the error description
   */
  public Description createErrorDescription(
      ErrorMessage errorMessage,
      @Nullable Tree suggestTree,
      Description.Builder descriptionBuilder,
      VisitorState state,
      @Nullable Symbol nonNullTarget) {
    Description.Builder builder = descriptionBuilder.setMessage(errorMessage.message);
    String checkName = CORE_CHECK_NAME;
    if (errorMessage.messageType.equals(GET_ON_EMPTY_OPTIONAL)) {
      checkName = OPTIONAL_CHECK_NAME;
    } else if (errorMessage.messageType.equals(FIELD_NO_INIT)
        || errorMessage.messageType.equals(METHOD_NO_INIT)
        || errorMessage.messageType.equals(NONNULL_FIELD_READ_BEFORE_INIT)) {
      checkName = INITIALIZATION_CHECK_NAME;
    }

    // Mildly expensive state.getPath() traversal, occurs only once per potentially
    // reported error.
    if (hasPathSuppression(state.getPath(), checkName)) {
      return Description.NO_MATCH;
    }

    if (config.suggestSuppressions() && suggestTree != null) {
      builder = addSuggestedSuppression(errorMessage, suggestTree, builder, state);
    }

    if (config.serializationIsActive()) {
      if (nonNullTarget != null) {
        SerializationService.serializeFixSuggestion(config, state, nonNullTarget, errorMessage);
      }
      // For the case of initializer errors, the leaf of state.getPath() may not be the field /
      // method on which the error is being reported (since we do a class-wide analysis to find such
      // errors).  In such cases, the suggestTree is the appropriate field / method tree, so use
      // that as the errorTree for serialization.
      Tree errorTree =
          (suggestTree != null
                  && (errorMessage.messageType.equals(FIELD_NO_INIT)
                      || errorMessage.messageType.equals(METHOD_NO_INIT)))
              ? suggestTree
              : state.getPath().getLeaf();
      SerializationService.serializeReportingError(
          config, state, errorTree, nonNullTarget, errorMessage);
    }

    // #letbuildersbuild
    return builder.build();
  }

  private static boolean canHaveSuppressWarningsAnnotation(Tree tree) {
    return tree instanceof MethodTree
        || (tree instanceof ClassTree && ((ClassTree) tree).getSimpleName().length() != 0)
        || tree instanceof VariableTree;
  }

  /**
   * Find out if a particular subchecker (e.g. NullAway.Optional) is being suppressed in a given
   * path.
   *
   * <p>This requires a tree path traversal, which is expensive, but we only do this when we would
   * otherwise report an error, which means this won't happen for most nodes/files.
   *
   * @param treePath The path with the error location as the leaf.
   * @param subcheckerName The string to check for inside @SuppressWarnings
   * @return Whether the subchecker is being suppressed at treePath.
   */
  private boolean hasPathSuppression(TreePath treePath, String subcheckerName) {
    return StreamSupport.stream(treePath.spliterator(), false)
        .filter(ErrorBuilder::canHaveSuppressWarningsAnnotation)
        .map(tree -> ASTHelpers.getSymbol(tree))
        .filter(symbol -> symbol != null)
        .anyMatch(
            symbol ->
                symbolHasSuppressWarningsAnnotation(symbol, subcheckerName)
                    || symbolIsExcludedClassSymbol(symbol));
  }

  private Description.Builder addSuggestedSuppression(
      ErrorMessage errorMessage,
      Tree suggestTree,
      Description.Builder builder,
      VisitorState state) {
    switch (errorMessage.messageType) {
      case DEREFERENCE_NULLABLE:
      case RETURN_NULLABLE:
      case PASS_NULLABLE:
      case ASSIGN_FIELD_NULLABLE:
      case SWITCH_EXPRESSION_NULLABLE:
        if (config.getCastToNonNullMethod() != null) {
          builder = addCastToNonNullFix(suggestTree, builder);
        } else {
          builder = addSuppressWarningsFix(suggestTree, builder, suppressionName);
        }
        break;
      case CAST_TO_NONNULL_ARG_NONNULL:
        builder = removeCastToNonNullFix(suggestTree, builder, state);
        break;
      case WRONG_OVERRIDE_RETURN:
        builder = addSuppressWarningsFix(suggestTree, builder, suppressionName);
        break;
      case WRONG_OVERRIDE_PARAM:
        builder = addSuppressWarningsFix(suggestTree, builder, suppressionName);
        break;
      case METHOD_NO_INIT:
      case FIELD_NO_INIT:
        builder = addSuppressWarningsFix(suggestTree, builder, INITIALIZATION_CHECK_NAME);
        break;
      case ANNOTATION_VALUE_INVALID:
        break;
      default:
        builder = addSuppressWarningsFix(suggestTree, builder, suppressionName);
    }
    return builder;
  }

  /**
   * create an error description for a generalized @Nullable value to @NonNull location assignment.
   *
   * <p>This includes: field assignments, method arguments and method returns
   *
   * @param errorMessage the error message object.
   * @param suggestTreeIfCastToNonNull the location at which a fix suggestion should be made if a
   *     castToNonNull method is available (usually the expression to cast)
   * @param descriptionBuilder the description builder for the error.
   * @param state the visitor state for the location which triggered the error (i.e. for suppression
   *     finding)
   * @param nonNullTarget if non-null, this error involved a pseudo-assignment of a @Nullable
   *     expression into a @NonNull target, and this parameter is the Symbol for that target.
   * @return the error description.
   */
  Description createErrorDescriptionForNullAssignment(
      ErrorMessage errorMessage,
      @Nullable Tree suggestTreeIfCastToNonNull,
      Description.Builder descriptionBuilder,
      VisitorState state,
      @Nullable Symbol nonNullTarget) {
    if (config.getCastToNonNullMethod() != null) {
      return createErrorDescription(
          errorMessage, suggestTreeIfCastToNonNull, descriptionBuilder, state, nonNullTarget);
    } else {
      return createErrorDescription(
          errorMessage,
          suppressibleNode(state.getPath()),
          descriptionBuilder,
          state,
          nonNullTarget);
    }
  }

  Description.Builder addSuppressWarningsFix(
      Tree suggestTree, Description.Builder builder, String suppressionName) {
    SuppressWarnings extantSuppressWarnings = null;
    Symbol treeSymbol = ASTHelpers.getSymbol(suggestTree);
    if (treeSymbol != null) {
      extantSuppressWarnings = treeSymbol.getAnnotation(SuppressWarnings.class);
    }
    SuggestedFix fix;
    if (extantSuppressWarnings == null) {
      fix =
          SuggestedFix.prefixWith(
              suggestTree,
              "@SuppressWarnings(\""
                  + suppressionName
                  + "\") "
                  + config.getAutofixSuppressionComment());
    } else {
      // need to update the existing list of warnings
      final List<String> suppressions = Lists.newArrayList(extantSuppressWarnings.value());
      suppressions.add(suppressionName);
      // find the existing annotation, so we can replace it
      final ModifiersTree modifiers =
          (suggestTree instanceof MethodTree)
              ? ((MethodTree) suggestTree).getModifiers()
              : ((VariableTree) suggestTree).getModifiers();
      final List<? extends AnnotationTree> annotations = modifiers.getAnnotations();
      // noinspection ConstantConditions
      com.google.common.base.Optional<? extends AnnotationTree> suppressWarningsAnnot =
          Iterables.tryFind(
              annotations,
              annot -> annot.getAnnotationType().toString().endsWith("SuppressWarnings"));
      if (!suppressWarningsAnnot.isPresent()) {
        throw new AssertionError("something went horribly wrong");
      }
      final String replacement =
          "@SuppressWarnings({"
              + Joiner.on(',').join(Iterables.transform(suppressions, s -> '"' + s + '"'))
              + "}) "
              + config.getAutofixSuppressionComment();
      fix = SuggestedFix.replace(suppressWarningsAnnot.get(), replacement);
    }
    return builder.addFix(fix);
  }

  /**
   * Adapted from {@link com.google.errorprone.fixes.SuggestedFixes}.
   *
   * <p>TODO: actually use {@link
   * com.google.errorprone.fixes.SuggestedFixes#addSuppressWarnings(VisitorState, String)} instead
   */
  @Nullable
  private Tree suppressibleNode(@Nullable TreePath path) {
    if (path == null) {
      return null;
    }
    return StreamSupport.stream(path.spliterator(), false)
        .filter(ErrorBuilder::canHaveSuppressWarningsAnnotation)
        .findFirst()
        .orElse(null);
  }

  private Description.Builder addCastToNonNullFix(Tree suggestTree, Description.Builder builder) {
    final String fullMethodName = config.getCastToNonNullMethod();
    if (fullMethodName == null) {
      throw new IllegalStateException("cast-to-non-null method not set");
    }
    // Add a call to castToNonNull around suggestTree:
    final String[] parts = fullMethodName.split("\\.");
    final String shortMethodName = parts[parts.length - 1];
    final String replacement = shortMethodName + "(" + suggestTree.toString() + ")";
    final SuggestedFix fix =
        SuggestedFix.builder()
            .replace(suggestTree, replacement)
            .addStaticImport(fullMethodName) // ensure castToNonNull static import
            .build();
    return builder.addFix(fix);
  }

  private Description.Builder removeCastToNonNullFix(
      Tree suggestTree, Description.Builder builder, VisitorState state) {
    // Note: Here suggestTree refers to the argument being cast. We need to find the
    // castToNonNull(...) invocation to be replaced by it. Fortunately, state.getPath()
    // should be currently pointing at said call.
    Tree currTree = state.getPath().getLeaf();
    Preconditions.checkArgument(
        currTree.getKind() == Tree.Kind.METHOD_INVOCATION,
        String.format("Expected castToNonNull invocation expression, found:\n%s", currTree));
    final MethodInvocationTree invTree = (MethodInvocationTree) currTree;
    Preconditions.checkArgument(
        invTree.getArguments().contains(suggestTree),
        String.format(
            "Method invocation tree %s does not contain the expression %s as an argument being cast",
            invTree, suggestTree));
    // Remove the call to castToNonNull:
    final SuggestedFix fix =
        SuggestedFix.builder().replace(invTree, suggestTree.toString()).build();
    return builder.addFix(fix);
  }

  /**
   * Reports initialization errors where a constructor fails to guarantee non-fields are initialized
   * along all paths at exit points.
   *
   * @param methodSymbol Constructor symbol.
   * @param message Error message.
   * @param state The VisitorState object.
   * @param descriptionBuilder the description builder for the error.
   * @param nonNullFields list of @Nonnull fields that are not guaranteed to be initialized along
   *     all paths at exit points of the constructor.
   */
  void reportInitializerError(
      Symbol.MethodSymbol methodSymbol,
      String message,
      VisitorState state,
      Description.Builder descriptionBuilder,
      ImmutableList<Symbol> nonNullFields) {
    // Check needed here, despite check in hasPathSuppression because initialization
    // checking happens at the class-level (meaning state.getPath() might not include the
    // method itself).
    if (symbolHasSuppressWarningsAnnotation(methodSymbol, INITIALIZATION_CHECK_NAME)) {
      return;
    }
    Tree methodTree = getTreesInstance(state).getTree(methodSymbol);
    ErrorMessage errorMessage = new ErrorMessage(METHOD_NO_INIT, message);
    state.reportMatch(
        createErrorDescription(errorMessage, methodTree, descriptionBuilder, state, null));
    if (config.serializationIsActive()) {
      // For now, we serialize each fix suggestion separately and measure their effectiveness
      // separately
      nonNullFields.forEach(
          symbol ->
              SerializationService.serializeFixSuggestion(config, state, symbol, errorMessage));
    }
  }

  boolean symbolHasSuppressWarningsAnnotation(Symbol symbol, String suppression) {
    SuppressWarnings annotation = symbol.getAnnotation(SuppressWarnings.class);
    if (annotation != null) {
      for (String s : annotation.value()) {
        // we need to check for standard suppression here also since we may report initialization
        // errors outside the normal ErrorProne match* methods
        if (s.equals(suppression) || allNames.stream().anyMatch(s::equals)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean symbolIsExcludedClassSymbol(Symbol symbol) {
    if (symbol instanceof Symbol.ClassSymbol) {
      ImmutableSet<String> excludedClassAnnotations = config.getExcludedClassAnnotations();
      return ((Symbol.ClassSymbol) symbol)
          .getAnnotationMirrors().stream()
              .map(anno -> anno.getAnnotationType().toString())
              .anyMatch(excludedClassAnnotations::contains);
    }
    return false;
  }

  static int getLineNumForElement(Element uninitField, VisitorState state) {
    Tree tree = getTreesInstance(state).getTree(uninitField);
    if (tree == null) {
      throw new RuntimeException(
          "When getting the line number for uninitialized field, can't get the tree from the element.");
    }
    DiagnosticPosition position =
        (DiagnosticPosition) tree; // Expect Tree to be JCTree and thus implement DiagnosticPosition
    TreePath path = state.getPath();
    JCCompilationUnit compilation = (JCCompilationUnit) path.getCompilationUnit();
    JavaFileObject file = compilation.getSourceFile();
    DiagnosticSource source = new DiagnosticSource(file, null);
    return source.getLineNumber(position.getStartPosition());
  }

  /**
   * Generate the message for uninitialized fields, including the line number for fields.
   *
   * @param uninitFields the set of uninitialized fields as the type of Element.
   * @param state the VisitorState object.
   * @return the error message for uninitialized fields with line numbers.
   */
  static String errMsgForInitializer(Set<Element> uninitFields, VisitorState state) {
    StringBuilder message = new StringBuilder("initializer method does not guarantee @NonNull ");
    Element uninitField;
    if (uninitFields.size() == 1) {
      uninitField = uninitFields.iterator().next();
      message.append("field ");
      message.append(uninitField.toString());
      message.append(" (line ");
      message.append(getLineNumForElement(uninitField, state));
      message.append(") is initialized");
    } else {
      message.append("fields ");
      Iterator<Element> it = uninitFields.iterator();
      while (it.hasNext()) {
        uninitField = it.next();
        message.append(
            uninitField.toString() + " (line " + getLineNumForElement(uninitField, state) + ")");
        if (it.hasNext()) {
          message.append(", ");
        } else {
          message.append(" are initialized");
        }
      }
    }
    message.append(
        " along all control-flow paths (remember to check for exceptions or early returns).");
    return message.toString();
  }

  void reportInitErrorOnField(Symbol symbol, VisitorState state, Description.Builder builder) {
    // Check needed here, despite check in hasPathSuppression because initialization
    // checking happens at the class-level (meaning state.getPath() might not include the
    // field itself).
    if (symbolHasSuppressWarningsAnnotation(symbol, INITIALIZATION_CHECK_NAME)) {
      return;
    }
    Tree tree = getTreesInstance(state).getTree(symbol);

    String fieldName = symbol.toString();

    if (symbol.enclClass().getNestingKind().isNested()) {
      String flatName = symbol.enclClass().flatName().toString();
      int index = flatName.lastIndexOf(".") + 1;
      fieldName = flatName.substring(index) + "." + fieldName;
    }

    if (isStatic(symbol)) {
      state.reportMatch(
          createErrorDescription(
              new ErrorMessage(
                  FIELD_NO_INIT, "@NonNull static field " + fieldName + " not initialized"),
              tree,
              builder,
              state,
              symbol));
    } else {
      state.reportMatch(
          createErrorDescription(
              new ErrorMessage(FIELD_NO_INIT, "@NonNull field " + fieldName + " not initialized"),
              tree,
              builder,
              state,
              symbol));
    }
  }
}
/*
 * Copyright (c) 2019 Uber Technologies, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.uber.nullaway;

import static com.uber.nullaway.ASTHelpersBackports.isStatic;
import static com.uber.nullaway.ErrorMessage.MessageTypes.FIELD_NO_INIT;
import static com.uber.nullaway.ErrorMessage.MessageTypes.GET_ON_EMPTY_OPTIONAL;
import static com.uber.nullaway.ErrorMessage.MessageTypes.METHOD_NO_INIT;
import static com.uber.nullaway.ErrorMessage.MessageTypes.NONNULL_FIELD_READ_BEFORE_INIT;
import static com.uber.nullaway.NullAway.CORE_CHECK_NAME;
import static com.uber.nullaway.NullAway.INITIALIZATION_CHECK_NAME;
import static com.uber.nullaway.NullAway.OPTIONAL_CHECK_NAME;
import static com.uber.nullaway.NullAway.getTreesInstance;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.uber.nullaway.fixserialization.SerializationService;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;

/** A class to construct error message to be displayed after the analysis finds error. */
public class ErrorBuilder {

  private final Config config;

  /** Checker name that can be used to suppress the warnings. */
  private final String suppressionName;

  /** Additional identifiers for this check, to be checked for in @SuppressWarnings annotations. */
  private final Set<String> allNames;

  ErrorBuilder(Config config, String suppressionName, Set<String> allNames) {
    this.config = config;
    this.suppressionName = suppressionName;
    this.allNames = allNames;
  }

  /**
   * create an error description for a nullability warning
   *
   * @param errorMessage the error message object.
   * @param descriptionBuilder the description builder for the error.
   * @param state the visitor state (used for e.g. suppression finding).
   * @param nonNullTarget if non-null, this error involved a pseudo-assignment of a @Nullable
   *     expression into a @NonNull target, and this parameter is the Symbol for that target.
   * @return the error description
   */
  Description createErrorDescription(
      ErrorMessage errorMessage,
      Description.Builder descriptionBuilder,
      VisitorState state,
      @Nullable Symbol nonNullTarget) {
    Tree enclosingSuppressTree = suppressibleNode(state.getPath());
    return createErrorDescription(
        errorMessage, enclosingSuppressTree, descriptionBuilder, state, nonNullTarget);
  }

  /**
   * create an error description for a nullability warning
   *
   * @param errorMessage the error message object.
   * @param suggestTree the location at which a fix suggestion should be made
   * @param descriptionBuilder the description builder for the error.
   * @param state the visitor state (used for e.g. suppression finding).
   * @param nonNullTarget if non-null, this error involved a pseudo-assignment of a @Nullable
   *     expression into a @NonNull target, and this parameter is the Symbol for that target.
   * @return the error description
   */
  public Description createErrorDescription(
      ErrorMessage errorMessage,
      @Nullable Tree suggestTree,
      Description.Builder descriptionBuilder,
      VisitorState state,
      @Nullable Symbol nonNullTarget) {
    Description.Builder builder = descriptionBuilder.setMessage(errorMessage.message);
    String checkName = CORE_CHECK_NAME;
    if (errorMessage.messageType.equals(GET_ON_EMPTY_OPTIONAL)) {
      checkName = OPTIONAL_CHECK_NAME;
    } else if (errorMessage.messageType.equals(FIELD_NO_INIT)
        || errorMessage.messageType.equals(METHOD_NO_INIT)
        || errorMessage.messageType.equals(NONNULL_FIELD_READ_BEFORE_INIT)) {
      checkName = INITIALIZATION_CHECK_NAME;
    }

    // Mildly expensive state.getPath() traversal, occurs only once per potentially
    // reported error.
    if (hasPathSuppression(state.getPath(), checkName)) {
      return Description.NO_MATCH;
    }

    if (config.suggestSuppressions() && suggestTree != null) {
      builder = addSuggestedSuppression(errorMessage, suggestTree, builder, state);
    }

    if (config.serializationIsActive()) {
      if (nonNullTarget != null) {
        SerializationService.serializeFixSuggestion(config, state, nonNullTarget, errorMessage);
      }
      // For the case of initializer errors, the leaf of state.getPath() may not be the field /
      // method on which the error is being reported (since we do a class-wide analysis to find such
      // errors).  In such cases, the suggestTree is the appropriate field / method tree, so use
      // that as the errorTree for serialization.
      Tree errorTree =
          (suggestTree != null
                  && (errorMessage.messageType.equals(FIELD_NO_INIT)
                      || errorMessage.messageType.equals(METHOD_NO_INIT)))
              ? suggestTree
              : state.getPath().getLeaf();
      SerializationService.serializeReportingError(
          config, state, errorTree, nonNullTarget, errorMessage);
    }

    // #letbuildersbuild
    return builder.build();
  }

  private static boolean canHaveSuppressWarningsAnnotation(Tree tree) {
    return tree instanceof MethodTree
        || (tree instanceof ClassTree && ((ClassTree) tree).getSimpleName().length() != 0)
        || tree instanceof VariableTree;
  }

  /**
   * Find out if a particular subchecker (e.g. NullAway.Optional) is being suppressed in a given
   * path.
   *
   * <p>This requires a tree path traversal, which is expensive, but we only do this when we would
   * otherwise report an error, which means this won't happen for most nodes/files.
   *
   * @param treePath The path with the error location as the leaf.
   * @param subcheckerName The string to check for inside @SuppressWarnings
   * @return Whether the subchecker is being suppressed at treePath.
   */
  private boolean hasPathSuppression(TreePath treePath, String subcheckerName) {
    return StreamSupport.stream(treePath.spliterator(), false)
        .filter(ErrorBuilder::canHaveSuppressWarningsAnnotation)
        .map(tree -> ASTHelpers.getSymbol(tree))
        .filter(symbol -> symbol != null)
        .anyMatch(
            symbol ->
                symbolHasSuppressWarningsAnnotation(symbol, subcheckerName)
                    || symbolIsExcludedClassSymbol(symbol));
  }

  private Description.Builder addSuggestedSuppression(
      ErrorMessage errorMessage,
      Tree suggestTree,
      Description.Builder builder,
      VisitorState state) {
    switch (errorMessage.messageType) {
      case DEREFERENCE_NULLABLE:
      case RETURN_NULLABLE:
      case PASS_NULLABLE:
      case ASSIGN_FIELD_NULLABLE:
      case SWITCH_EXPRESSION_NULLABLE:
        if (config.getCastToNonNullMethod() != null) {
          builder = addCastToNonNullFix(suggestTree, builder);
        } else {
          builder = addSuppressWarningsFix(suggestTree, builder, suppressionName);
        }
        break;
      case CAST_TO_NONNULL_ARG_NONNULL:
        builder = removeCastToNonNullFix(suggestTree, builder, state);
        break;
      case WRONG_OVERRIDE_RETURN:
        builder = addSuppressWarningsFix(suggestTree, builder, suppressionName);
        break;
      case WRONG_OVERRIDE_PARAM:
        builder = addSuppressWarningsFix(suggestTree, builder, suppressionName);
        break;
      case METHOD_NO_INIT:
      case FIELD_NO_INIT:
        builder = addSuppressWarningsFix(suggestTree, builder, INITIALIZATION_CHECK_NAME);
        break;
      case ANNOTATION_VALUE_INVALID:
        break;
      default:
        builder = addSuppressWarningsFix(suggestTree, builder, suppressionName);
    }
    return builder;
  }

  /**
   * create an error description for a generalized @Nullable value to @NonNull location assignment.
   *
   * <p>This includes: field assignments, method arguments and method returns
   *
   * @param errorMessage the error message object.
   * @param suggestTreeIfCastToNonNull the location at which a fix suggestion should be made if a
   *     castToNonNull method is available (usually the expression to cast)
   * @param descriptionBuilder the description builder for the error.
   * @param state the visitor state for the location which triggered the error (i.e. for suppression
   *     finding)
   * @param nonNullTarget if non-null, this error involved a pseudo-assignment of a @Nullable
   *     expression into a @NonNull target, and this parameter is the Symbol for that target.
   * @return the error description.
   */
  Description createErrorDescriptionForNullAssignment(
      ErrorMessage errorMessage,
      @Nullable Tree suggestTreeIfCastToNonNull,
      Description.Builder descriptionBuilder,
      VisitorState state,
      @Nullable Symbol nonNullTarget) {
    if (config.getCastToNonNullMethod() != null) {
      return createErrorDescription(
          errorMessage, suggestTreeIfCastToNonNull, descriptionBuilder, state, nonNullTarget);
    } else {
      return createErrorDescription(
          errorMessage,
          suppressibleNode(state.getPath()),
          descriptionBuilder,
          state,
          nonNullTarget);
    }
  }

  Description.Builder addSuppressWarningsFix(
      Tree suggestTree, Description.Builder builder, String suppressionName) {
    SuppressWarnings extantSuppressWarnings = null;
    Symbol treeSymbol = ASTHelpers.getSymbol(suggestTree);
    if (treeSymbol != null) {
      extantSuppressWarnings = treeSymbol.getAnnotation(SuppressWarnings.class);
    }
    SuggestedFix fix;
    if (extantSuppressWarnings == null) {
      fix =
          SuggestedFix.prefixWith(
              suggestTree,
              "@SuppressWarnings(\""
                  + suppressionName
                  + "\") "
                  + config.getAutofixSuppressionComment());
    } else {
      // need to update the existing list of warnings
      final List<String> suppressions = Lists.newArrayList(extantSuppressWarnings.value());
      suppressions.add(suppressionName);
      // find the existing annotation, so we can replace it
      final ModifiersTree modifiers =
          (suggestTree instanceof MethodTree)
              ? ((MethodTree) suggestTree).getModifiers()
              : ((VariableTree) suggestTree).getModifiers();
      final List<? extends AnnotationTree> annotations = modifiers.getAnnotations();
      // noinspection ConstantConditions
      com.google.common.base.Optional<? extends AnnotationTree> suppressWarningsAnnot =
          Iterables.tryFind(
              annotations,
              annot -> annot.getAnnotationType().toString().endsWith("SuppressWarnings"));
      if (!suppressWarningsAnnot.isPresent()) {
        throw new AssertionError("something went horribly wrong");
      }
      final String replacement =
          "@SuppressWarnings({"
              + Joiner.on(',').join(Iterables.transform(suppressions, s -> '"' + s + '"'))
              + "}) "
              + config.getAutofixSuppressionComment();
      fix = SuggestedFix.replace(suppressWarningsAnnot.get(), replacement);
    }
    return builder.addFix(fix);
  }

  /**
   * Adapted from {@link com.google.errorprone.fixes.SuggestedFixes}.
   *
   * <p>TODO: actually use {@link
   * com.google.errorprone.fixes.SuggestedFixes#addSuppressWarnings(VisitorState, String)} instead
   */
  @Nullable
  private Tree suppressibleNode(@Nullable TreePath path) {
    if (path == null) {
      return null;
    }
    return StreamSupport.stream(path.spliterator(), false)
        .filter(ErrorBuilder::canHaveSuppressWarningsAnnotation)
        .findFirst()
        .orElse(null);
  }

  private Description.Builder addCastToNonNullFix(Tree suggestTree, Description.Builder builder) {
    final String fullMethodName = config.getCastToNonNullMethod();
    if (fullMethodName == null) {
      throw new IllegalStateException("cast-to-non-null method not set");
    }
    // Add a call to castToNonNull around suggestTree:
    final String[] parts = fullMethodName.split("\\.");
    final String shortMethodName = parts[parts.length - 1];
    final String replacement = shortMethodName + "(" + suggestTree.toString() + ")";
    final SuggestedFix fix =
        SuggestedFix.builder()
            .replace(suggestTree, replacement)
            .addStaticImport(fullMethodName) // ensure castToNonNull static import
            .build();
    return builder.addFix(fix);
  }

  private Description.Builder removeCastToNonNullFix(
      Tree suggestTree, Description.Builder builder, VisitorState state) {
    // Note: Here suggestTree refers to the argument being cast. We need to find the
    // castToNonNull(...) invocation to be replaced by it. Fortunately, state.getPath()
    // should be currently pointing at said call.
    Tree currTree = state.getPath().getLeaf();
    Preconditions.checkArgument(
        currTree.getKind() == Tree.Kind.METHOD_INVOCATION,
        String.format("Expected castToNonNull invocation expression, found:\n%s", currTree));
    final MethodInvocationTree invTree = (MethodInvocationTree) currTree;
    Preconditions.checkArgument(
        invTree.getArguments().contains(suggestTree),
        String.format(
            "Method invocation tree %s does not contain the expression %s as an argument being cast",
            invTree, suggestTree));
    // Remove the call to castToNonNull:
    final SuggestedFix fix =
        SuggestedFix.builder().replace(invTree, suggestTree.toString()).build();
    return builder.addFix(fix);
  }

  /**
   * Reports initialization errors where a constructor fails to guarantee non-fields are initialized
   * along all paths at exit points.
   *
   * @param methodSymbol Constructor symbol.
   * @param message Error message.
   * @param state The VisitorState object.
   * @param descriptionBuilder the description builder for the error.
   * @param nonNullFields list of @Nonnull fields that are not guaranteed to be initialized along
   *     all paths at exit points of the constructor.
   */
  void reportInitializerError(
      Symbol.MethodSymbol methodSymbol,
      String message,
      VisitorState state,
      Description.Builder descriptionBuilder,
      ImmutableList<Symbol> nonNullFields) {
    // Check needed here, despite check in hasPathSuppression because initialization
    // checking happens at the class-level (meaning state.getPath() might not include the
    // method itself).
    if (symbolHasSuppressWarningsAnnotation(methodSymbol, INITIALIZATION_CHECK_NAME)) {
      return;
    }
    Tree methodTree = getTreesInstance(state).getTree(methodSymbol);
    ErrorMessage errorMessage = new ErrorMessage(METHOD_NO_INIT, message);
    state.reportMatch(
        createErrorDescription(errorMessage, methodTree, descriptionBuilder, state, null));
    if (config.serializationIsActive()) {
      // For now, we serialize each fix suggestion separately and measure their effectiveness
      // separately
      nonNullFields.forEach(
          symbol ->
              SerializationService.serializeFixSuggestion(config, state, symbol, errorMessage));
    }
  }

  boolean symbolHasSuppressWarningsAnnotation(Symbol symbol, String suppression) {
    SuppressWarnings annotation = symbol.getAnnotation(SuppressWarnings.class);
    if (annotation != null) {
      for (String s : annotation.value()) {
        // we need to check for standard suppression here also since we may report initialization
        // errors outside the normal ErrorProne match* methods
        if (s.equals(suppression) || allNames.stream().anyMatch(s::equals)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean symbolIsExcludedClassSymbol(Symbol symbol) {
    if (symbol instanceof Symbol.ClassSymbol) {
      ImmutableSet<String> excludedClassAnnotations = config.getExcludedClassAnnotations();
      return ((Symbol.ClassSymbol) symbol)
          .getAnnotationMirrors().stream()
              .map(anno -> anno.getAnnotationType().toString())
              .anyMatch(excludedClassAnnotations::contains);
    }
    return false;
  }

  static int getLineNumForElement(Element uninitField, VisitorState state) {
    Tree tree = getTreesInstance(state).getTree(uninitField);
    if (tree == null) {
      throw new RuntimeException(
          "When getting the line number for uninitialized field, can't get the tree from the element.");
    }
    DiagnosticPosition position =
        (DiagnosticPosition) tree; // Expect Tree to be JCTree and thus implement DiagnosticPosition
    TreePath path = state.getPath();
    JCCompilationUnit compilation = (JCCompilationUnit) path.getCompilationUnit();
    JavaFileObject file = compilation.getSourceFile();
    DiagnosticSource source = new DiagnosticSource(file, null);
    return source.getLineNumber(position.getStartPosition());
  }

  /**
   * Generate the message for uninitialized fields, including the line number for fields.
   *
   * @param uninitFields the set of uninitialized fields as the type of Element.
   * @param state the VisitorState object.
   * @return the error message for uninitialized fields with line numbers.
   */
  static String errMsgForInitializer(Set<Element> uninitFields, VisitorState state) {
    StringBuilder message = new StringBuilder("initializer method does not guarantee @NonNull ");
    Element uninitField;
    if (uninitFields.size() == 1) {
      uninitField = uninitFields.iterator().next();
      message.append("field ");
      message.append(uninitField.toString());
      message.append(" (line ");
      message.append(getLineNumForElement(uninitField, state));
      message.append(") is initialized");
    } else {
      message.append("fields ");
      Iterator<Element> it = uninitFields.iterator();
      while (it.hasNext()) {
        uninitField = it.next();
        message.append(
            uninitField.toString() + " (line " + getLineNumForElement(uninitField, state) + ")");
        if (it.hasNext()) {
          message.append(", ");
        } else {
          message.append(" are initialized");
        }
      }
    }
    message.append(
        " along all control-flow paths (remember to check for exceptions or early returns).");
    return message.toString();
  }

  void reportInitErrorOnField(Symbol symbol, VisitorState state, Description.Builder builder) {
    // Check needed here, despite check in hasPathSuppression because initialization
    // checking happens at the class-level (meaning state.getPath() might not include the
    // field itself).
    if (symbolHasSuppressWarningsAnnotation(symbol, INITIALIZATION_CHECK_NAME)) {
      return;
    }
    Tree tree = getTreesInstance(state).getTree(symbol);

    String fieldName = symbol.toString();

    if (symbol.enclClass().getNestingKind().isNested()) {
      String flatName = symbol.enclClass().flatName().toString();
      int index = flatName.lastIndexOf(".") + 1;
      fieldName = flatName.substring(index) + "." + fieldName;
    }

    if (isStatic(symbol)) {
      state.reportMatch(
          createErrorDescription(
              new ErrorMessage(
                  FIELD_NO_INIT, "@NonNull static field " + fieldName + " not initialized"),
              tree,
              builder,
              state,
              symbol));
    } else {
      state.reportMatch(
          createErrorDescription(
              new ErrorMessage(FIELD_NO_INIT, "@NonNull field " + fieldName + " not initialized"),
              tree,
              builder,
              state,
              symbol));
    }
  }
}
/*
 * Copyright (c) 2019 Uber Technologies, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.uber.nullaway;

import static com.uber.nullaway.ASTHelpersBackports.isStatic;
import static com.uber.nullaway.ErrorMessage.MessageTypes.FIELD_NO_INIT;
import static com.uber.nullaway.ErrorMessage.MessageTypes.GET_ON_EMPTY_OPTIONAL;
import static com.uber.nullaway.ErrorMessage.MessageTypes.METHOD_NO_INIT;
import static com.uber.nullaway.ErrorMessage.MessageTypes.NONNULL_FIELD_READ_BEFORE_INIT;
import static com.uber.nullaway.NullAway.CORE_CHECK_NAME;
import static com.uber.nullaway.NullAway.INITIALIZATION_CHECK_NAME;
import static com.uber.nullaway.NullAway.OPTIONAL_CHECK_NAME;
import static com.uber.nullaway.NullAway.getTreesInstance;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.uber.nullaway.fixserialization.SerializationService;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;

/** A class to construct error message to be displayed after the analysis finds error. */
public class ErrorBuilder {

  private final Config config;

  /** Checker name that can be used to suppress the warnings. */
  private final String suppressionName;

  /** Additional identifiers for this check, to be checked for in @SuppressWarnings annotations. */
  private final Set<String> allNames;

  ErrorBuilder(Config config, String suppressionName, Set<String> allNames) {
    this.config = config;
    this.suppressionName = suppressionName;
    this.allNames = allNames;
  }

  /**
   * create an error description for a nullability warning
   *
   * @param errorMessage the error message object.
   * @param descriptionBuilder the description builder for the error.
   * @param state the visitor state (used for e.g. suppression finding).
   * @param nonNullTarget if non-null, this error involved a pseudo-assignment of a @Nullable
   *     expression into a @NonNull target, and this parameter is the Symbol for that target.
   * @return the error description
   */
  Description createErrorDescription(
      ErrorMessage errorMessage,
      Description.Builder descriptionBuilder,
      VisitorState state,
      @Nullable Symbol nonNullTarget) {
    Tree enclosingSuppressTree = suppressibleNode(state.getPath());
    return createErrorDescription(
        errorMessage, enclosingSuppressTree, descriptionBuilder, state, nonNullTarget);
  }

  /**
   * create an error description for a nullability warning
   *
   * @param errorMessage the error message object.
   * @param suggestTree the location at which a fix suggestion should be made
   * @param descriptionBuilder the description builder for the error.
   * @param state the visitor state (used for e.g. suppression finding).
   * @param nonNullTarget if non-null, this error involved a pseudo-assignment of a @Nullable
   *     expression into a @NonNull target, and this parameter is the Symbol for that target.
   * @return the error description
   */
  public Description createErrorDescription(
      ErrorMessage errorMessage,
      @Nullable Tree suggestTree,
      Description.Builder descriptionBuilder,
      VisitorState state,
      @Nullable Symbol nonNullTarget) {
    Description.Builder builder = descriptionBuilder.setMessage(errorMessage.message);
    String checkName = CORE_CHECK_NAME;
    if (errorMessage.messageType.equals(GET_ON_EMPTY_OPTIONAL)) {
      checkName = OPTIONAL_CHECK_NAME;
    } else if (errorMessage.messageType.equals(FIELD_NO_INIT)
        || errorMessage.messageType.equals(METHOD_NO_INIT)
        || errorMessage.messageType.equals(NONNULL_FIELD_READ_BEFORE_INIT)) {
      checkName = INITIALIZATION_CHECK_NAME;
    }

    // Mildly expensive state.getPath() traversal, occurs only once per potentially
    // reported error.
    if (hasPathSuppression(state.getPath(), checkName)) {
      return Description.NO_MATCH;
    }

    if (config.suggestSuppressions() && suggestTree != null) {
      builder = addSuggestedSuppression(errorMessage, suggestTree, builder, state);
    }

    if (config.serializationIsActive()) {
      if (nonNullTarget != null) {
        SerializationService.serializeFixSuggestion(config, state, nonNullTarget, errorMessage);
      }
      // For the case of initializer errors, the leaf of state.getPath() may not be the field /
      // method on which the error is being reported (since we do a class-wide analysis to find such
      // errors).  In such cases, the suggestTree is the appropriate field / method tree, so use
      // that as the errorTree for serialization.
      Tree errorTree =
          (suggestTree != null
                  && (errorMessage.messageType.equals(FIELD_NO_INIT)
                      || errorMessage.messageType.equals(METHOD_NO_INIT)))
              ? suggestTree
              : state.getPath().getLeaf();
      SerializationService.serializeReportingError(
          config, state, errorTree, nonNullTarget, errorMessage);
    }

    // #letbuildersbuild
    return builder.build();
  }

  private static boolean canHaveSuppressWarningsAnnotation(Tree tree) {
    return tree instanceof MethodTree
        || (tree instanceof ClassTree && ((ClassTree) tree).getSimpleName().length() != 0)
        || tree instanceof VariableTree;
  }

  /**
   * Find out if a particular subchecker (e.g. NullAway.Optional) is being suppressed in a given
   * path.
   *
   * <p>This requires a tree path traversal, which is expensive, but we only do this when we would
   * otherwise report an error, which means this won't happen for most nodes/files.
   *
   * @param treePath The path with the error location as the leaf.
   * @param subcheckerName The string to check for inside @SuppressWarnings
   * @return Whether the subchecker is being suppressed at treePath.
   */
  private boolean hasPathSuppression(TreePath treePath, String subcheckerName) {
    return StreamSupport.stream(treePath.spliterator(), false)
        .filter(ErrorBuilder::canHaveSuppressWarningsAnnotation)
        .map(tree -> ASTHelpers.getSymbol(tree))
        .filter(symbol -> symbol != null)
        .anyMatch(
            symbol ->
                symbolHasSuppressWarningsAnnotation(symbol, subcheckerName)
                    || symbolIsExcludedClassSymbol(symbol));
  }

  private Description.Builder addSuggestedSuppression(
      ErrorMessage errorMessage,
      Tree suggestTree,
      Description.Builder builder,
      VisitorState state) {
    switch (errorMessage.messageType) {
      case DEREFERENCE_NULLABLE:
      case RETURN_NULLABLE:
      case PASS_NULLABLE:
      case ASSIGN_FIELD_NULLABLE:
      case SWITCH_EXPRESSION_NULLABLE:
        if (config.getCastToNonNullMethod() != null) {
          builder = addCastToNonNullFix(suggestTree, builder);
        } else {
          builder = addSuppressWarningsFix(suggestTree, builder, suppressionName);
        }
        break;
      case CAST_TO_NONNULL_ARG_NONNULL:
        builder = removeCastToNonNullFix(suggestTree, builder, state);
        break;
      case WRONG_OVERRIDE_RETURN:
        builder = addSuppressWarningsFix(suggestTree, builder, suppressionName);
        break;
      case WRONG_OVERRIDE_PARAM:
        builder = addSuppressWarningsFix(suggestTree, builder, suppressionName);
        break;
      case METHOD_NO_INIT:
      case FIELD_NO_INIT:
        builder = addSuppressWarningsFix(suggestTree, builder, INITIALIZATION_CHECK_NAME);
        break;
      case ANNOTATION_VALUE_INVALID:
        break;
      default:
        builder = addSuppressWarningsFix(suggestTree, builder, suppressionName);
    }
    return builder;
  }

  /**
   * create an error description for a generalized @Nullable value to @NonNull location assignment.
   *
   * <p>This includes: field assignments, method arguments and method returns
   *
   * @param errorMessage the error message object.
   * @param suggestTreeIfCastToNonNull the location at which a fix suggestion should be made if a
   *     castToNonNull method is available (usually the expression to cast)
   * @param descriptionBuilder the description builder for the error.
   * @param state the visitor state for the location which triggered the error (i.e. for suppression
   *     finding)
   * @param nonNullTarget if non-null, this error involved a pseudo-assignment of a @Nullable
   *     expression into a @NonNull target, and this parameter is the Symbol for that target.
   * @return the error description.
   */
  Description createErrorDescriptionForNullAssignment(
      ErrorMessage errorMessage,
      @Nullable Tree suggestTreeIfCastToNonNull,
      Description.Builder descriptionBuilder,
      VisitorState state,
      @Nullable Symbol nonNullTarget) {
    if (config.getCastToNonNullMethod() != null) {
      return createErrorDescription(
          errorMessage, suggestTreeIfCastToNonNull, descriptionBuilder, state, nonNullTarget);
    } else {
      return createErrorDescription(
          errorMessage,
          suppressibleNode(state.getPath()),
          descriptionBuilder,
          state,
          nonNullTarget);
    }
  }

  Description.Builder addSuppressWarningsFix(
      Tree suggestTree, Description.Builder builder, String suppressionName) {
    SuppressWarnings extantSuppressWarnings = null;
    Symbol treeSymbol = ASTHelpers.getSymbol(suggestTree);
    if (treeSymbol != null) {
      extantSuppressWarnings = treeSymbol.getAnnotation(SuppressWarnings.class);
    }
    SuggestedFix fix;
    if (extantSuppressWarnings == null) {
      fix =
          SuggestedFix.prefixWith(
              suggestTree,
              "@SuppressWarnings(\""
                  + suppressionName
                  + "\") "
                  + config.getAutofixSuppressionComment());
    } else {
      // need to update the existing list of warnings
      final List<String> suppressions = Lists.newArrayList(extantSuppressWarnings.value());
      suppressions.add(suppressionName);
      // find the existing annotation, so we can replace it
      final ModifiersTree modifiers =
          (suggestTree instanceof MethodTree)
              ? ((MethodTree) suggestTree).getModifiers()
              : ((VariableTree) suggestTree).getModifiers();
      final List<? extends AnnotationTree> annotations = modifiers.getAnnotations();
      // noinspection ConstantConditions
      com.google.common.base.Optional<? extends AnnotationTree> suppressWarningsAnnot =
          Iterables.tryFind(
              annotations,
              annot -> annot.getAnnotationType().toString().endsWith("SuppressWarnings"));
      if (!suppressWarningsAnnot.isPresent()) {
        throw new AssertionError("something went horribly wrong");
      }
      final String replacement =
          "@SuppressWarnings({"
              + Joiner.on(',').join(Iterables.transform(suppressions, s -> '"' + s + '"'))
              + "}) "
              + config.getAutofixSuppressionComment();
      fix = SuggestedFix.replace(suppressWarningsAnnot.get(), replacement);
    }
    return builder.addFix(fix);
  }

  /**
   * Adapted from {@link com.google.errorprone.fixes.SuggestedFixes}.
   *
   * <p>TODO: actually use {@link
   * com.google.errorprone.fixes.SuggestedFixes#addSuppressWarnings(VisitorState, String)} instead
   */
  @Nullable
  private Tree suppressibleNode(@Nullable TreePath path) {
    if (path == null) {
      return null;
    }
    return StreamSupport.stream(path.spliterator(), false)
        .filter(ErrorBuilder::canHaveSuppressWarningsAnnotation)
        .findFirst()
        .orElse(null);
  }

  private Description.Builder addCastToNonNullFix(Tree suggestTree, Description.Builder builder) {
    final String fullMethodName = config.getCastToNonNullMethod();
    if (fullMethodName == null) {
      throw new IllegalStateException("cast-to-non-null method not set");
    }
    // Add a call to castToNonNull around suggestTree:
    final String[] parts = fullMethodName.split("\\.");
    final String shortMethodName = parts[parts.length - 1];
    final String replacement = shortMethodName + "(" + suggestTree.toString() + ")";
    final SuggestedFix fix =
        SuggestedFix.builder()
            .replace(suggestTree, replacement)
            .addStaticImport(fullMethodName) // ensure castToNonNull static import
            .build();
    return builder.addFix(fix);
  }

  private Description.Builder removeCastToNonNullFix(
      Tree suggestTree, Description.Builder builder, VisitorState state) {
    // Note: Here suggestTree refers to the argument being cast. We need to find the
    // castToNonNull(...) invocation to be replaced by it. Fortunately, state.getPath()
    // should be currently pointing at said call.
    Tree currTree = state.getPath().getLeaf();
    Preconditions.checkArgument(
        currTree.getKind() == Tree.Kind.METHOD_INVOCATION,
        String.format("Expected castToNonNull invocation expression, found:\n%s", currTree));
    final MethodInvocationTree invTree = (MethodInvocationTree) currTree;
    Preconditions.checkArgument(
        invTree.getArguments().contains(suggestTree),
        String.format(
            "Method invocation tree %s does not contain the expression %s as an argument being cast",
            invTree, suggestTree));
    // Remove the call to castToNonNull:
    final SuggestedFix fix =
        SuggestedFix.builder().replace(invTree, suggestTree.toString()).build();
    return builder.addFix(fix);
  }

  /**
   * Reports initialization errors where a constructor fails to guarantee non-fields are initialized
   * along all paths at exit points.
   *
   * @param methodSymbol Constructor symbol.
   * @param message Error message.
   * @param state The VisitorState object.
   * @param descriptionBuilder the description builder for the error.
   * @param nonNullFields list of @Nonnull fields that are not guaranteed to be initialized along
   *     all paths at exit points of the constructor.
   */
  void reportInitializerError(
      Symbol.MethodSymbol methodSymbol,
      String message,
      VisitorState state,
      Description.Builder descriptionBuilder,
      ImmutableList<Symbol> nonNullFields) {
    // Check needed here, despite check in hasPathSuppression because initialization
    // checking happens at the class-level (meaning state.getPath() might not include the
    // method itself).
    if (symbolHasSuppressWarningsAnnotation(methodSymbol, INITIALIZATION_CHECK_NAME)) {
      return;
    }
    Tree methodTree = getTreesInstance(state).getTree(methodSymbol);
    ErrorMessage errorMessage = new ErrorMessage(METHOD_NO_INIT, message);
    state.reportMatch(
        createErrorDescription(errorMessage, methodTree, descriptionBuilder, state, null));
    if (config.serializationIsActive()) {
      // For now, we serialize each fix suggestion separately and measure their effectiveness
      // separately
      nonNullFields.forEach(
          symbol ->
              SerializationService.serializeFixSuggestion(config, state, symbol, errorMessage));
    }
  }

  boolean symbolHasSuppressWarningsAnnotation(Symbol symbol, String suppression) {
    SuppressWarnings annotation = symbol.getAnnotation(SuppressWarnings.class);
    if (annotation != null) {
      for (String s : annotation.value()) {
        // we need to check for standard suppression here also since we may report initialization
        // errors outside the normal ErrorProne match* methods
        if (s.equals(suppression) || allNames.stream().anyMatch(s::equals)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean symbolIsExcludedClassSymbol(Symbol symbol) {
    if (symbol instanceof Symbol.ClassSymbol) {
      ImmutableSet<String> excludedClassAnnotations = config.getExcludedClassAnnotations();
      return ((Symbol.ClassSymbol) symbol)
          .getAnnotationMirrors().stream()
              .map(anno -> anno.getAnnotationType().toString())
              .anyMatch(excludedClassAnnotations::contains);
    }
    return false;
  }

  static int getLineNumForElement(Element uninitField, VisitorState state) {
    Tree tree = getTreesInstance(state).getTree(uninitField);
    if (tree == null) {
      throw new RuntimeException(
          "When getting the line number for uninitialized field, can't get the tree from the element.");
    }
    DiagnosticPosition position =
        (DiagnosticPosition) tree; // Expect Tree to be JCTree and thus implement DiagnosticPosition
    TreePath path = state.getPath();
    JCCompilationUnit compilation = (JCCompilationUnit) path.getCompilationUnit();
    JavaFileObject file = compilation.getSourceFile();
    DiagnosticSource source = new DiagnosticSource(file, null);
    return source.getLineNumber(position.getStartPosition());
  }

  /**
   * Generate the message for uninitialized fields, including the line number for fields.
   *
   * @param uninitFields the set of uninitialized fields as the type of Element.
   * @param state the VisitorState object.
   * @return the error message for uninitialized fields with line numbers.
   */
  static String errMsgForInitializer(Set<Element> uninitFields, VisitorState state) {
    StringBuilder message = new StringBuilder("initializer method does not guarantee @NonNull ");
    Element uninitField;
    if (uninitFields.size() == 1) {
      uninitField = uninitFields.iterator().next();
      message.append("field ");
      message.append(uninitField.toString());
      message.append(" (line ");
      message.append(getLineNumForElement(uninitField, state));
      message.append(") is initialized");
    } else {
      message.append("fields ");
      Iterator<Element> it = uninitFields.iterator();
      while (it.hasNext()) {
        uninitField = it.next();
        message.append(
            uninitField.toString() + " (line " + getLineNumForElement(uninitField, state) + ")");
        if (it.hasNext()) {
          message.append(", ");
        } else {
          message.append(" are initialized");
        }
      }
    }
    message.append(
        " along all control-flow paths (remember to check for exceptions or early returns).");
    return message.toString();
  }

  void reportInitErrorOnField(Symbol symbol, VisitorState state, Description.Builder builder) {
    // Check needed here, despite check in hasPathSuppression because initialization
    // checking happens at the class-level (meaning state.getPath() might not include the
    // field itself).
    if (symbolHasSuppressWarningsAnnotation(symbol, INITIALIZATION_CHECK_NAME)) {
      return;
    }
    Tree tree = getTreesInstance(state).getTree(symbol);

    String fieldName = symbol.toString();

    if (symbol.enclClass().getNestingKind().isNested()) {
      String flatName = symbol.enclClass().flatName().toString();
      int index = flatName.lastIndexOf(".") + 1;
      fieldName = flatName.substring(index) + "." + fieldName;
    }

    if (isStatic(symbol)) {
      state.reportMatch(
          createErrorDescription(
              new ErrorMessage(
                  FIELD_NO_INIT, "@NonNull static field " + fieldName + " not initialized"),
              tree,
              builder,
              state,
              symbol));
    } else {
      state.reportMatch(
          createErrorDescription(
              new ErrorMessage(FIELD_NO_INIT, "@NonNull field " + fieldName + " not initialized"),
              tree,
              builder,
              state,
              symbol));
    }
  }
}
