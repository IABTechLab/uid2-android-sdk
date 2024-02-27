
# Prepare next snapshot
echo "Setting next snapshot version $NEW_SNAPSHOT_VERSION"
sed -i.bak "s/${NEW_VERSION}/${NEW_SNAPSHOT_VERSION}/g" gradle.properties
