regex="INSTANCE hipie_plugins.UseDataset.UseDataset\(Ins001\): LABEL\(\"Use Dataset\"\),VERSION\(\"\d.\d\"\)\n.+LogicalFilename=\"file\"\n.+Method=\"THOR\"\n.+Structure=\"STRING\|Field1\"\nEND"
regex1="INSTANCE HIPIE_Plugins.UseDataset.UseDataset\(Ins001\):?\s*LogicalFilename\s*=\"file\"\s*Method=THOR\s*Structure=STRING|Field1\s*END"
regex2="INSTANCE HIPIE_Plugins.UseDataset.UseDataset\(Ins001\): LABEL\(\"Use Dataset\"\),VERSION\(\"\d.\d(.\d)?\"\)\n.+LogicalFilename=\"file\"\n.+Method=\"THOR\"\n.+Structure=\"STRING\|Field1\"\nEND"

## Update this to the path where the compositions are.
path="/home/woodbr01/dev_git_06142016"

# Find all the files that contain the ghost dataset
# Count the number of files contining the string
numberOfFiles=$(grep -Pzonrl "$regex" $path | wc -l)
numberOfFiles+=$(grep -Pzonrl "$regex1" $path | wc -l)
numberOfFiles+=$(grep -Pzonrl "$regex2" $path | wc -l)

# List the files
echo "The list of files are:"

grep -Pzonrl "$regex" $path
grep -Pzonrl "$regex1" $path
grep -Pzonrl "$regex2" $path
echo ""
echo "Found $numberOfFiles files with ghost use dataset"

# Remove all the ghost dataset blocks
echo ""
perl -i -0pe "s/$regex//" $path/*/*.cmp
perl -i -0pe "s/$regex1//" $path/*/*.cmp
perl -i -0pe "s/$regex2//" $path/*/*.cmp
echo ""


# Report back number of files deleted and if any are remaining.
numberOfFilesAfter=$(grep -Pzonrl "$regex" $path | wc -l)
numberOfFiles+=$(grep -Pzonrl "$regex1" $path | wc -l)
numberOfFiles+=$(grep -Pzonrl "$regex2" $path | wc -l)

COUNT=`expr $numberOfFiles - $numberOfFilesAfter`

echo "Removed $COUNT occurances of ghost use dataset. $numberOfFilesAfter remaining."
