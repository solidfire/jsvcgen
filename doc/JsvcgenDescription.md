JSON-RPC Description Format
===========================

This is the format for specifying your web service to `jsvcgen`.
It is JSON-based and uses a mix of concepts from [json-schema][json-schema], [JSON-WSP][JSON-WSP] and
 [Swagger][Swagger-spec].
The file extension is simply `.json`.

Definitions
===========

### HTTP Verbs

Because JSON-RPC does not *need* to be run over HTTP, there is not any support for using different
 [HTTP methods](http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html).
The only HTTP verb JSON-RPC uses is `POST`.

### HTTP Status Codes

JSON-RPC has a system of rich error notifications, which means there is no explicit need for HTTP status codes.
However, when running JSON-RPC over HTTP, it is generally considered best practice to use `200` to indicated success and
 some other form of an error code (in the `4xx` or `5xx` range) to indicate an error to the client.
The full list of status codes can be seen at [RFC 2616](http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html).

### MIME Type

The official MIME type is `"application/json+jsvcgen-description"`, but `"application/json"` is perfectly fine.

Specification
=============

Format <a name="format" />
--------------------------

| Common Name | JSON Type | `jsvcgen` Type | Comments                                                                  |
|-------------|-----------|----------------|---------------------------------------------------------------------------|
| float       | `number`  | `float`        |                                                                           |
| double      | `number`  | `float`        |                                                                           |
| number      | `number`  | `number`       |                                                                           |
| integer     | `number`  | `integer`      | JSON only supports floating-point, so this is a [restriction](#Restriction). |
| string      | `string`  | `string`       |                                                                           |
| boolean     | `boolean` | `boolean`      |                                                                           |
| array       | `array`   | `["${type}"]`  | Arrays are uniform. Use an [`alias`](#TypeDefinition) to restrict lengths.|

Schema
------

#### Does Order Matter?

The order of arrays specifying things like `"members"` of a [type definition](#TypeDefinition), `"params"` of a
 [method](#Method), `"types"` of a [service description](#ServiceDescription) and so on matters only a little.
When using a code or documentation generator, the output order is typically the same as the input, so the order you
 define your parameters in the JSON document will end up the same in your Java output.

#### Extension Fields

For each description object, you are allowed to add any extra fields you want and they will simply be ignored.
However, for future-proofing your description files, it is a good idea to prefix your custom fields with `x-`.
The contents of any extension fields are unchecked and can be any valid JSON value.

### `ServiceDescription` <a name="ServiceDescription" />

The root of a `jsvcgen` document is the `ServiceDescription`.

| Field Name       | Type             | Description                                                                    |
|------------------|------------------|--------------------------------------------------------------------------------|
| `"type"`         | `string`         | *Required.* Should be `"application/json+jsvcgen-description"`.                |
| `"servicename"`  | `string`         | *Required.* The name of the service. This name is used in code generation.     |
| `"host"`         | `string`         | *Required.* Hostname of the service. This can be a [pattern](#pattern).        |
| `"endpoint"`     | `string`         | *Required.* Host-relative endpoint to connect to. This can be a [pattern](#pattern). |
| `"schemes"`      | `array[string]`  | An array of supported URL schemes. Default value is `["http"]`.                |
| `"version"`      | `string`         | The API version -- this is typically part of the URL. Default value is `"1.0"`.|
| `"documentation"`| [`Documentation`](#Documentation) | Global documentation for the API.                               |
| `"types"`        | `array[`[`TypeDefinition`](#TypeDefinition)`]` | Type definitions not necessarily tied with a particular method. Default value is `[]`. |
| `"methods"`      | `array[`[`Method`](#Method)`]` | List of methods this API provides. The default value is `[]`, but this is not very useful. |

##### Example

```js
{
    "type": "application/json+jsvcgen-description",
    "version": "1.2",
    "servicename": "UserService",
    "host": "${kerberosHost}",
    "endpoint": "/json-rpc/${version}/",
    "schemes": [ "https" ],
    "documentation": [
        "An API for controlling Kerberos users and groups."
    ],
    "types": [ ... ],
    "methods": [ ... ]
}
```

### `Documentation` <a name="Documentation" />

Documentation fields can be found on almost all types of description objects.
The purpose is to provide users with some description of what a [method](#Method) does, what a [type](#TypeDefinition)
 represents, what a [parameter](#Parameter) or [member](#Member) means and so on.
You should provide useful documentation -- your users will thank you.

A documentation object can be either a `string` or an array of `string`s.
There is no particular meaning associated with a single break in the string, so you can think of the strings being
 joined back together by placing a single space (`" "`) between them.
If there is a blank line in the documentation array, it is treated as the start of a new paragraph.

##### Example

```js
[
    "Complex documentation can be split into an array for ease of maintenance.",
    "You can break it up however you want.",
    "",
    "Leave a blank \"line\" to start a new paragraph."
]
```

### `Member` <a name="Member" />

A member is used inside of a [type definition](#TypeDefinition) to describe specific elements of a *structure*.
The `"name"` will be the key in the structure, the `"type"` describes the value.

| Field Name       | Type             | Description                                                                    |
|------------------|------------------|--------------------------------------------------------------------------------|
| `"name"`         | `string`         | *Required.* The name of this member. It is expected (but not required) to be `^[a-zA-Z_][a-zA-Z_0-9]*$`. |
| `"type"`         | [`TypeUse`](#TypeUse) | *Required.* The type of this member.                                      |
| `"documentation"`| [`Documentation`](#Documentation) | Documentation for this member.                                |

#### Example

```js
"members": [
    { "name": "username",  "type": "string" },
    { "name": "user_id",   "type": "UserID" },
    {
        "name":          "mobile",
        "type":          "PhoneNumber",
        "documentation": ["A mobile phone number for the user."]
    },
    { "name": "age",        "type": "number" },
    { "name": "given_name", "type": "string" },
    { "name": "surname",    "type": "string" }
]
```

### `Method` <a name="Method" />

A method describes an operation the service can provide.
The value of `"name"` will be used as the `"method"` in the JSON-RPC request.

| Field Name       | Type             | Description                                                                    |
|------------------|------------------|--------------------------------------------------------------------------------|
| `"name"`         | `string`         | *Required.* The name of this method. It is expected (but not required) to be `^[a-zA-Z_][a-zA-Z_0-9]*$`. |
| `"documentation"`| [`Documentation`](#Documentation) | Documentation for this method.                                |
| `"params"`       | `array[`[`TypeUse`](#TypeUse)`]` | An array of parameters this method accepts. By default, the method will accept no parameters (`{}`). |
| `"returnInfo"`  | [`ReturnInfo`](#ReturnInfo) | Information about the result of calling this method. By default, the method does not return information (`void`). |

### `Restriction` <a name="Restriction" />

Allows for refinement of an [*alias*](#TypeDefinition) by restricting its values by certain criteria.
All fields are optional, meaning the default value of `{}` is an unrestricted type.
The names and meanings of the restrictions are taken from [json-schema](http://json-schema.org/latest/json-schema-validation.html)

| Field Name       | Type             | Description                                                                    |
|------------------|------------------|--------------------------------------------------------------------------------|
| `"maximum"`      | `number`         | [The maximum value for a number.](#Restriction-maximum)                        |
| `"exclusiveMaximum"` | `boolean`    | [Is the `"maximum"` value exclusive?](#Restriction-maximum)                    |
| `"minimum"`      | `number`         | [The minimum value for a number.](#Restriction-minimum)                        |
| `"exclusiveMinimum"` | `boolean`    | [Is the `"minimum"` value exclusive?](#Restriction-minimum)                    |
| `"maxLength"`    | `integer`        | [The maximum length of a `string`.](#Restriction-maxLength)                    |
| `"minLength"`    | `integer`        | [The minimum length of a `string`.](#Restriction-minLength)                    |
| `"maxItems"`     | `integer`        | [The maximum length of an array.](#Restriction-maxItems)                       |
| `"minItems"`     | `integer`        | [The minimum length of an array.](#Restriction-minItems)                       |
| `"uniqueItems"`  | `boolean`        | [Do the elements of an array have to be unique?](#Restriction-uniqueItems)     |
| `"enum"`         | `array[`[`EnumSpecification`](#Restriction-enum)`]` | [Should the elements of the array be restricted to certain values?](#Restriction-enum) |
| `"multipleOf"`   | `number`         | [Does a number have to be a multiple of something?](#Restriction-multipleOf)   |

#### `"maximum"` and `"exclusiveMaximum"` <a name="Restriction-maximum" />

 - [json-schema §5.1.2: maximum and exclusiveMaximum](http://json-schema.org/latest/json-schema-validation.html#anchor17)

The upper limit for a numeric value.
The field `"exclusiveMaximum"` only applies if `"maximum"` is present.
If `"exclusiveMaximum"` is `false`, the value is allowed to include the value specified in `"maximum"`.
The default value for `"exclusiveMaximum"` is `false`, meaning the default behavior is inclusive.

##### Example

```js
"restriction": {
    "maximum": 10,
    "exclusiveMaximum": true
}
```

#### `"minimum"` and `"exclusiveMinimum"` <a name="Restriction-minimum" />

 - [json-schema §5.1.3: minimum and exclusiveMinimum](http://json-schema.org/latest/json-schema-validation.html#anchor21)

The lower limit for a numeric value.
The field `"exclusiveMinimum"` only applies if `"minimum"` is present.
If `"exclusiveMinimum"` is `false`, the value is allowed to include the value specified in `"minimum"`.
The default value for `"exclusiveMinimum"` is `false`, meaning the default behavior is inclusive.

##### Example

```js
"restriction": {
    "minimum": 0,
    "exclusiveMinimum": false
}
```

#### `"maxLength"` and `"minLength"` <a name="Restriction-maxLength" /> <a name="Restriction-minLength" />

 - [json-schema §5.2.1: maxLength](http://json-schema.org/latest/json-schema-validation.html#anchor26)
 - [json-schema §5.2.2: minLength](http://json-schema.org/latest/json-schema-validation.html#anchor29)

The minimum and maximum length of a `string` (for arrays, see [`"minItems"`](#Restriction-minItems) and
 [`"maxItems"`](#Restriction-maxItems).
Both numbers must be greater than or equal to 0.
It is highly recommended that `"minLength"` be less than or equal to `"maxLength"`, but this is not required.

##### Example

```js
"restriction": {
    "minLength":  8,
    "maxLength": 20
}
```

#### `"pattern"` <a name="Restriction-pattern" />

 - [json-schema §5.2.3: pattern](http://json-schema.org/latest/json-schema-validation.html#anchor33)

A regular expression to restrict the string value by.
This restriction is only valid on `string`s.
The supported regex syntax is [ECMAScript](http://www.regular-expressions.info/javascript.html).

##### Example

```js
"restriction": {
    "pattern": "^[A-Z][a-z]+$"
}
```

#### `"maxItems"` and `"minItems"` <a name="Restriction-maxItems" /> <a name="Restriction-minItems" />

 - [json-schema §5.3.2: maxItems](http://json-schema.org/latest/json-schema-validation.html#anchor42)
 - [json-schema §5.3.3: minItems](http://json-schema.org/latest/json-schema-validation.html#anchor45)

The minimum and maximum length of an array (for `string`s, see [`"minLength"`](#Restriction-minLength) and
 [`"maxLength"`](#Restriction-maxLength).
Both numbers must be greater than or equal to 0.
It is highly recommended that `"minItems"` be less than or equal to `"maxItems"`, but this is not required.

##### Example

```js
"restriction": {
    "minItems":  3,
    "maxItems": 15
}
```

#### `"uniqueItems"` <a name="Restriction-uniqueItems" />

 - [json-schema §5.3.4: uniqueItems](http://json-schema.org/latest/json-schema-validation.html#anchor49)

For an array, do the items in the array have to be unique?
By default, this is `false`.

#### `"enum"` <a name="Restriction-enum" />

 - [json-schema §5.5.1: uniqueItems](http://json-schema.org/latest/json-schema-validation.html#anchor76)

Restricts a type to a specific subset of possible values.
This is typically used on `string` types, but can be applied to any other type.

| Field Name       | Type             | Description                                                                    |
|------------------|------------------|--------------------------------------------------------------------------------|
| `"value"`        | any              | *Required.* An allowed value -- should be the same type as what the [type definition](#TypeDefinition) is aliasing. |
| `"documentation"`| [`Documentation`](#Documentation) | Documentation about this value.                               |

If you do not wish to include documentation for your values, you can forgo the object wrapper and use the value for
 `"value"` directly in the array.
This is *not* allowed if your value type is an object itself.

##### Example

```js
"restriction": {
    "enum": [
        {
            "value": "apple",
            "documentation": "An apple is the pomaceous fruit of the apple tree."
        },
        {
            "value": "banana",
            "documentation": "A yellow fruit made in a factory."
        },
        {
            "value": "crayon",
            "documentation": "A delicious, healthy snack."
        }
    ]
}
```

Shorthand without documentation:

```js
"restriction": {
    "enum": ["apple", "banana", "crayon"]
}
```

#### `"multipleOf"` <a name="Restriction-multipleOf" />

 - [json-schema §5.1.1: multipleOf](http://json-schema.org/latest/json-schema-validation.html#anchor14)

Restrict a numeric value to be a multiple of some other number.

##### Example

```js
"restriction": {
    "multipleOf": 5
}
```

### `ReturnInfo` <a name="ReturnInfo" />

Describes the result of calling a [method](#Method).

| Field Name       | Type             | Description                                                                    |
|------------------|------------------|--------------------------------------------------------------------------------|
| `"type"`         | [`TypeUse`](#TypeUse) | *Required.* The type this method returns.                                 |
| `"documentation"`| [`Documentation`](#Documentation) | Documentation about what this method returns.                 |

##### Example

```js
{
    "type": ["string"],
    "documentation": "The list of groups the user is a member of."
}
```

### `TypeDefinition` <a name="TypeDefinition" />

A `TypeDefinition` is a free-floating type not necessarily associated with a [method](#Method), but are referred to by
 [method](#Method)s.
There are two major kinds of definitions: a *structure* and an *alias*.
A *structure* defines a JSON object with a list of [member](#Member)s.
An *alias* allows you to refer to another type via a different name and allows refinement of that type through a
 [restriction](#Restriction).

| Field Name       | Type             | Description                                                                    |
|------------------|------------------|--------------------------------------------------------------------------------|
| `"name"`         | `string`         | *Required.* The name of this type. It will be referred to in other places by name. It is expected (but not required) to be `[a-zA-Z_][a-zA-Z_0-9]*`. |
| `"members"`      | `array[`[`Member`](#Member)`]` | *Required for structures.* A list of members for this *structure*. |
| `"alias"`        | [`TypeUse`](#TypeUse) | *Required for aliases.* The type to alias.                                |
| `"restriction"`  | [`Restriction`](#Restriction) | When this is an alias, refine values by this criteria.            |
| `"documentation"`| [`Documentation`](#Documentation) | Type-level documentation.                                     |

##### Example

A *structure*:

```js
{
    "name": "User",
    "documentation": [
        "A user is a system contact.",
        "They are probably a real person, but might be a robot.",
        "You never know these days."
    ],
    "members": [
        { "name": "username",  "type": "string" },
        { "name": "user_id",   "type": "UserID" },
        {
          "name":          "mobile",
          "type":          "PhoneNumber",
          "documentation": ["A mobile phone number for the user."]
        },
        { "name": "age",        "type": "number" },
        { "name": "given_name", "type": "string" },
        { "name": "surname",    "type": "string" }
    ]
}
```

An *alias*:

```js
{
    "name": "PhoneNumber",
    "alias": "string",
    "restriction": {
        "pattern": "[0-9]{3}-[0-9]{3}-[0-9]{4}"
    }
}
```

### `TypeUse` <a name="TypeUse" />

| Field Name       | Type             | Description                                                                    |
|------------------|------------------|--------------------------------------------------------------------------------|
| `"name"`         | `string` or `array[string]` | *Required.* The name of the type -- refers to a [type definition](#TypeDefinition). |
| `"optional"`     | `boolean`        | Is this [parameter](#Parameter) or [member](#Member) optional? By default, this is `false`. |

##### Example

To simply specify a type (`integer`):

```js
"integer"
```

An array of `integer`s:

```js
["integer"]
```

An optional `integer`:

```js
{ "name": "integer", "optional": true }
```

An optional array of `integer`s:

```js
{ "name": ["integer"], "optional": true }
```

 [json-schema]: http://json-schema.org/
 [JSON-WSP]: http://en.wikipedia.org/wiki/JSON-WSP
 [Swagger-spec]: https://github.com/wordnik/swagger-spec
