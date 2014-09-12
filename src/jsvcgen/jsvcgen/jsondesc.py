"""Copyright (c) 2014 by Travis Gockel. All rights reserved.

This program is free software: you can redistribute it and/or modify it under the terms of the Apache License
as published by the Apache Software Foundation, either version 2 of the License, or (at your option) any later
version.

Travis Gockel (travis@gockelhut.com)"""
import json
import logging

class LoadContext():
    debug    = logging.debug
    info     = logging.info
    warning  = logging.warning
    critical = logging.critical

def get_documentation(doc_lines):
    out = ''
    for line in doc_lines:
        if line:
            out += line + ' '
        else:
            out += '\n'
    return out

class Member():
    def __init__(self, name, type, documentation):
        self._name = name
        self._type = type
        self._documentation = documentation
    
    @classmethod
    def load_from_json(cls, name, src, context):
        return cls(name,
                   Type.load_from_json(None, src, context),
                   get_documentation(src.get('doc_lines', [])) if isinstance(src, dict) else ''
                  )
    
    @property
    def documentation(self):
        return self._documentation
    
    @property
    def name(self):
        return self._name
    
    @property
    def type(self):
        return self._type
    
    @property
    def typename(self):
        return self._type.name

class Argument(Member):
    pass

class Method():
    def __init__(self, name, params, return_info, documentation):
        self._name = name
        self._params = params
        self._return_info = return_info
        self._documentation = documentation
    
    @classmethod
    def load_from_json(cls, name, src, context):
        params = ParamList.load_from_json(name + 'Request', src.get('params', {}), context)
        return_info = ReturnInfo.load_from_json(name, src.get('ret_info', {}), context)
        documentation = get_documentation(src.get('doc_lines', []))
        return cls(name, params, return_info, documentation)
    
    @property
    def documentation(self):
        return self._documentation
    
    @property
    def name(self):
        return self._name
    
    @property
    def return_info(self):
        return self._return_info
    
    @property
    def params(self):
        return self._params
    
    def __repr__(self):
        return self._name + '(' + ', '.join((x.name for x in self._params._members)) + '): ' \
             + repr(self._return_info._type)

class ReturnInfo():
    def __init__(self, name, type, documentation):
        self._name = name
        self._type = type
        self._documentation = documentation
    
    @classmethod
    def load_from_json(cls, name, src, context):
        return cls(name,
                   Type.load_from_json(name + 'Result', src.get('type'), context),
                   get_documentation(src.get('doc_lines', []))
                  )
    
    @property
    def type(self):
        return self._type

class Type():
    def __init__(self, name, type, members, documentation, is_array=False, is_optional=False):
        self._name = name
        self._type = type
        self._members = members
        self._documentation = documentation
        self._is_array = is_array
        self._is_optional = is_optional
    
    @classmethod
    def load_from_json(cls, name, src, context):
        if isinstance(src, dict):
            name = src.get('type', name)
            type = object
            members = { memberName : Member.load_from_json(memberName, val, context)
                        for memberName, val in src.get('members', {}).items()
                      }
            documentation = get_documentation(src.get('doc_lines', []))
            is_optional = src.get('optional', False)
        elif isinstance(src, str):
            name = src
            type = src
            members = None
            documentation = ''
            is_optional = False
        elif isinstance(src, list):
            assert(len(src) == 1)
            typ = cls.load_from_json(name, src[0], context)
            typ._is_array = True
            return typ
        else:
            raise ValueError('Type must be defined by a dict or string, found {}'.format(src))
        return cls(name, type, members, documentation, is_optional=is_optional)
    
    @property
    def documentation(self):
        return self._documentation
    
    @property
    def is_array(self):
        return self._is_array
    
    @property
    def is_optional(self):
        return self._is_optional
    
    @property
    def members(self):
        return self._members.values()
    
    @property
    def name(self):
        return self._name
    
    def __repr__(self):
        ss = self._name + '\n'                             \
           + '\t\t' + self._documentation + '\n'
        if self._members is None:
            ss += repr(self._type)
        else:
            ss += '\t' + '\n\t'.join(map(repr, self._members))
        if self._is_array:
            ss += '[]'
        return ss

class ParamList():
    def __init__(self, name, args):
        self._name = name
        self._args = args
    
    @classmethod
    def load_from_json(cls, name, src, context):
        args = []
        for argname, argdesc in sorted(src.items(), key=lambda x: x[1].get('def_order', len(src))):
            args.append(Argument.load_from_json(argname, argdesc, context))
        return cls(name, args)
    
    @property
    def args(self):
        return self._args
    
    def __iter__(self):
        return iter(self._args)

class ServiceDescription():
    def __init__(self, name, types=None, methods=None):
        self._name = name
        self._types = types or []
        self._methods = methods or []
    
    @classmethod
    def load_from_json(cls, src, context=LoadContext()):
        types = []
        typenames = set([])
        methods = []
        methodnames = set([])
        
        for name, val in src["types"].items():
            if name in typenames:
                context.warning('Found duplicate type with name "{}"', name)
            typ = Type.load_from_json(name=name, src=val, context=context)
            typenames.add(typ.name)
            types.append(typ)
        
        for name, val in src['methods'].items():
            if name in methodnames:
                context.warning('Found duplicate method with name "{}"', name)
            method = Method.load_from_json(name, val, context)
            methodnames.add(method.name)
            methods.append(method)
        
        return cls(name=src['servicename'],
                   types=types,
                   methods=methods
                  )
    
    @property
    def is_array(self):
        return False
    
    @property
    def methods(self):
        return self._methods
    
    @property
    def name(self):
        return self._name
    
    @property
    def types(self):
        return self._types
    
    def __repr__(self):
        return '\n\n'.join(map(repr, self._types))   \
             + '\n\n'                                \
             + '\n\n'.join(map(repr, self._methods))
