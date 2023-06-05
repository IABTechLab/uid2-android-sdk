# Google's GMA uses reflection to identify available adapters. These are therefore not referenced by the consuming
# application and thus, require a rule to ensure that they are not stripped out during the build.
-keep public class com.uid2.securesignals.gma.*
