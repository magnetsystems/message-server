run ./print_schema.sh

This will iterate through all the classes in com.magnet.mmx.protocol and generate schemas in JSON using names of the fields or values of
"@SerializedName" gson annotation.

Caveat: Some classes are not in proper POJO format, i.e. class members are absent. For those, empty set will be printed.
Interface classes are skipped.
