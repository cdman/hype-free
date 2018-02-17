import StringIO

def _normalize_dict(d):
    '''
    Strips the ending _ from the keys of the dictionary recursively
    '''
    result = {}
    for k, v in d.iteritems():
        if isinstance(k, basestring) and k.endswith(''):
	    k = k[:-1]
	if isinstance(v, dict):
	    v = _normalize_dict(v)
	result[k] = v
    return result

def cleanup(request, response):
    '''
    Convert request and response dicts to a human readable format where
    possible.
    '''
    request_short = None
    response_short = None
    miss = 0

    request = _normalize_dict(request)
    response = _normalize_dict(response)
    if 'MemcacheGetRequest' in request:
        request = request['MemcacheGetRequest']
        response = response['MemcacheGetResponse']
        request_short = memcache_get(request)
        response_short, miss = memcache_get_response(response)
    elif 'MemcacheSetRequest' in request:
        request_short = memcache_set(request['MemcacheSetRequest'])
    elif 'Query' in request:
        request_short = datastore_query(request['Query'])
    elif 'GetRequest' in request:
        request_short = datastore_get(request['GetRequest'])
    elif 'PutRequest' in request:
        request_short = datastore_put(request['PutRequest'])
    # todo:
    # TaskQueueBulkAddRequest
    # BeginTransaction
    # Transaction

    return request_short, response_short, miss

def memcache_get_response(response):
    response_miss = 0
    items = response['item']
    for i, item in enumerate(items):
        if type(item) == dict:
            item = item['MemcacheGetResponse_Item']['value']
            item = truncate(repr(item))
            items[i] = item
    response_short = '\n'.join(items)
    if not items:
        response_miss = 1
    return response_short, response_miss

def memcache_get(request):
    keys = request['key']
    request_short = '\n'.join([truncate(k) for k in keys])
    namespace = ''
    if 'name_space' in request:
        namespace = request['name_space']
        if len(keys) > 1:
            request_short += '\n'
        else:
            request_short += ' '
        request_short += '(ns:%s)' % truncate(namespace)
    return request_short

def memcache_set(request):
    keys = [truncate(i['MemcacheSetRequest_Item']['key']) for i in request['item']]
    return '\n'.join(keys)

def datastore_query(query):
    kind = query.get('kind', 'UnknownKind')
    count = query.get('count', '')

    filters_clean = datastore_query_filter(query)
    orders_clean = datastore_query_order(query)

    s = StringIO.StringIO()
    s.write('SELECT FROM %s\n' % kind)
    if filters_clean:
        s.write('WHERE\n')
        for name, op, value in filters_clean:
            s.write('%s %s %s\n' % (name, op, value))
    if orders_clean:
        s.write('ORDER BY\n')
        for prop, direction in orders_clean:
            s.write('%s %s\n' % (prop, direction))
    if count:
        s.write('LIMIT %s\n' % count)

    result = s.getvalue()
    s.close()
    return result

def datastore_query_filter(query):
    _Operator_NAMES = {
        0: '?',
        1: '<',
        2: '<=',
        3: '>',
        4: '>=',
        5: '=',
        6: 'IN',
        7: 'EXISTS',
    }
    filters = query.get('filter', [])
    filters_clean = []
    for f in filters:
        if 'Query_Filter' not in f:
            continue
        f = f['Query_Filter']
        op = _Operator_NAMES[int(f.get('op', 0))]
        props = f['property']
        for p in props:
            p = p['Property']
            name = p.get('name', 'UnknownName')

            if 'value' in p:

                propval = p['value']['PropertyValue']

                if 'stringvalue' in propval:
                    value = propval['stringvalue']
                elif 'referencevalue' in propval:
                    ref = propval['referencevalue']['PropertyValue_ReferenceValue']
                    els = ref['pathelement']
                    paths = []
                    for el in els:
                        path = el['PropertyValue_ReferenceValuePathElement']
                        paths.append('%s(%s)' % (path['type'], id_or_name(path)))
                    value = '->'.join(paths)
                elif 'booleanvalue' in propval:
                    value = propval['booleanvalue']
                elif 'uservalue' in propval:
                    value = 'User(' + propval['uservalue']['PropertyValue_UserValue']['email'] + ')'
                elif '...' in propval:
                    value = '...'
                elif 'int64value' in propval:
                    value = propval['int64value']
                else:
                    raise Exception(propval)
            else:
                value = ''
            filters_clean.append((name, op, value))
    return filters_clean

def datastore_query_order(query):
    orders = query.get('order', [])
    _Direction_NAMES = {
        0: '?DIR',
        1: 'ASC',
        2: 'DESC',
    }
    orders_clean = []
    for order in orders:
        order = order['Query_Order']
        direction = _Direction_NAMES[int(order.get('direction', 0))]
        prop = order.get('property', 'UnknownProperty')
        orders_clean.append((prop, direction))
    return orders_clean

def id_or_name(path):
    if 'name' in path:
        return path['name']
    else:
        return path['id']

def datastore_get(request):
    keys = request['key']
    if len(keys) > 1:
        keylist = cleanup_key(keys.pop(0))
        for key in keys:
            keylist += ', ' + cleanup_key(key)
        return keylist
    elif keys:
        return cleanup_key(keys[0])

def cleanup_key(key):
    if 'Reference' not in key: 
        #sometimes key is passed in as '...'
        return key
    els = key['Reference']['path']['Path']['element']
    paths = []
    for el in els:
        path = el['Path_Element']
        paths.append('%s(%s)' % (path['type'] if 'type' in path 
                     else 'UnknownType', id_or_name(path)))
    return '->'.join(paths)

def datastore_put(request):
    entities = request['entity']
    keys = []
    for entity in entities:
        keys.append(cleanup_key(entity['EntityProto']['key']))
    return '\n'.join(keys)

def truncate(value, limit=100):
    if len(value) > limit:
        return value[:limit - 3] + '...'
    else:
        return value
