"""Batería de pruebas E2E contra la arquitectura levantada con Docker Compose.
Sólo hace peticiones HTTP al API Gateway (y verifica que los puertos internos NO sean accesibles).
Uso: python -I pruebas_e2e.py [fase]
fase = principal (por defecto) | caida | tras_caida | reinicio
"""
import base64, hashlib, hmac, json, sys, threading, time, urllib.request, urllib.error

GW = "http://localhost:8080/api/v1"
SVC = {"usuario": "http://localhost:8081", "categoria": "http://localhost:8082",
       "producto": "http://localhost:8083", "boleta": "http://localhost:8084",
       "detalle": "http://localhost:8085"}
SECRET_ANTIGUO_REPO = "dG9rZW5TZWNyZXRLZXlTb2Z0d2FyZVRpZW5kYTIwMjZQY0hhcmR3YXJlU2VjdXJpdHk="
U = str(int(time.time()))[-6:]
RES = []


def req(method, url, body=None, token=None, headers=None, raw=None):
    h = {"Content-Type": "application/json", "Accept": "application/json"}
    if token: h["Authorization"] = "Bearer " + token
    if headers: h.update(headers)
    data = raw.encode() if raw is not None else (json.dumps(body).encode() if body is not None else None)
    r = urllib.request.Request(url, data=data, method=method, headers=h)
    try:
        with urllib.request.urlopen(r, timeout=20) as resp:
            txt = resp.read().decode("utf-8", "replace"); st = resp.status
    except urllib.error.HTTPError as e:
        txt = e.read().decode("utf-8", "replace"); st = e.code
    except Exception as e:
        return 0, str(e)
    try: return st, json.loads(txt) if txt else None
    except ValueError: return st, txt


def chk(cod, desc, cond, esperado, obtenido):
    RES.append((cod, "OK " if cond else "FALLA", desc, str(esperado), str(obtenido)[:110]))


def err(b):
    return (b or {}).get("error", "") if isinstance(b, dict) else b


def login(email, pwd):
    st, b = req("POST", GW + "/auth/login", {"email": email, "password": pwd})
    return st, (b or {}).get("token") if isinstance(b, dict) else None, b


def jwt_payload(t):
    p = t.split(".")[1]; p += "=" * (-len(p) % 4)
    return json.loads(base64.urlsafe_b64decode(p))


def forjar_jwt(payload, secreto_b64):
    b64 = lambda d: base64.urlsafe_b64encode(d).rstrip(b"=").decode()
    h = b64(json.dumps({"alg": "HS256"}).encode()); p = b64(json.dumps(payload).encode())
    sig = hmac.new(base64.b64decode(secreto_b64), f"{h}.{p}".encode(), hashlib.sha256).digest()
    return f"{h}.{p}.{b64(sig)}"


def stock(pid):
    st, b = req("GET", f"{GW}/productos/{pid}")
    return b.get("stock") if isinstance(b, dict) else None


def crear_producto(tok, sku, stock_ini, cat=1, activo=True, precio=10000):
    return req("POST", GW + "/productos", {"sku": sku, "nombre": "Prueba " + sku, "precio": precio,
               "stock": stock_ini, "idCategoria": cat, "activo": activo}, tok)


def comprar(tok, items, extra=None):
    body = {"items": [{"idProducto": i, "cantidad": c} for i, c in items]}
    body.update(extra or {})
    return req("POST", GW + "/boletas", body, tok)


def principal():
    # ---------- R: requerimientos de la rúbrica / instrucciones ----------
    st, _ = req("GET", "http://localhost:8080/v3/api-docs")
    chk("R01", "Swagger del gateway disponible", st == 200, 200, st)
    st, adm, b = login("admin@duoc.cl", "admin")
    chk("R02", "Login ADMIN", st == 200 and adm, "200 + token", st)
    st, ven, _ = login("vendedor@duoc.cl", "vendedor")
    chk("R03", "Login VENDEDOR", st == 200 and ven, "200 + token", st)
    st, cli, bcli = login("alumno@duoc.cl", "1234")
    chk("R04", "Login CLIENTE", st == 200 and cli, "200 + token", st)
    pl = jwt_payload(adm)
    chk("R05", "JWT contiene rol, userId y expiración", {"rol", "userId", "exp"} <= set(pl), "rol,userId,exp", sorted(pl))
    for ruta in ["/productos", "/productos/activos", "/productos/1", "/categorias", "/categorias/1"]:
        st, _ = req("GET", GW + ruta)
        chk("R06", f"GET público {ruta}", st == 200, 200, st)
    st, c = req("POST", GW + "/categorias", {"nombre": "Cat " + U, "descripcion": "x"}, adm)
    cid = c.get("id") if isinstance(c, dict) else None
    chk("R07", "CRUD categoría: crear", st == 201, 201, st)
    st, _ = req("PUT", f"{GW}/categorias/{cid}", {"nombre": "Cat2 " + U}, adm)
    chk("R07", "CRUD categoría: actualizar", st == 200, 200, st)
    st, _ = req("DELETE", f"{GW}/categorias/{cid}", token=adm)
    chk("R07", "CRUD categoría: eliminar", st == 204, 204, st)
    st, p = crear_producto(adm, "T-" + U, 5)
    pid = p.get("id") if isinstance(p, dict) else None
    chk("R08", "CRUD producto: crear", st == 201, 201, st)
    st, _ = req("PUT", f"{GW}/productos/{pid}", {"sku": "T-" + U, "nombre": "Mod", "precio": 1, "stock": 5, "idCategoria": 1}, adm)
    chk("R08", "CRUD producto: actualizar", st == 200, 200, st)
    st, _ = req("DELETE", f"{GW}/productos/{pid}", token=adm)
    chk("R08", "CRUD producto: eliminar", st == 204, 204, st)
    st, _ = req("GET", f"{GW}/productos/999999")
    chk("R08", "Producto inexistente devuelve 404", st == 404, 404, st)
    st, u = req("POST", GW + "/usuarios", {"email": f"u{U}@test.cl", "password": "xxxx", "nombre": "U", "rol": "VENDEDOR"}, adm)
    uid = u.get("id") if isinstance(u, dict) else None
    chk("R09", "CRUD usuario: crear", st == 201, 201, st)
    st, _ = req("PUT", f"{GW}/usuarios/{uid}", {"email": f"u{U}@test.cl", "nombre": "U2"}, adm)
    chk("R09", "CRUD usuario: actualizar", st == 200, 200, st)
    st, _ = req("DELETE", f"{GW}/usuarios/{uid}", token=adm)
    chk("R09", "CRUD usuario: eliminar", st == 204, 204, st)
    matriz = [
        ("VENDEDOR", ven, "GET", "/boletas", 200), ("VENDEDOR", ven, "GET", "/boletas/1", 200),
        ("VENDEDOR", ven, "GET", "/detalles", 200), ("VENDEDOR", ven, "GET", "/usuarios", 403),
        ("VENDEDOR", ven, "POST", "/productos", 403), ("VENDEDOR", ven, "DELETE", "/categorias/1", 403),
        ("CLIENTE", cli, "GET", "/boletas", 403), ("CLIENTE", cli, "GET", "/usuarios", 403),
        ("CLIENTE", cli, "POST", "/productos", 403), ("CLIENTE", cli, "GET", "/boletas/mis-boletas", 200),
        ("ANÓNIMO", None, "GET", "/boletas", 401), ("ANÓNIMO", None, "POST", "/productos", 401),
        ("ANÓNIMO", None, "GET", "/usuarios", 401),
    ]
    for rol, tok, m, ruta, esp in matriz:
        st, _ = req(m, GW + ruta, {} if m == "POST" else None, tok)
        chk("R10", f"Rol {rol}: {m} {ruta}", st == esp, esp, st)
    st, p = crear_producto(adm, "C-" + U, 10)
    pc = p["id"]
    st, bol = comprar(cli, [(pc, 2)], {"total": 20000})
    chk("R11", "Compra CLIENTE crea boleta", st == 201, 201, st)
    chk("R11", "Compra descuenta stock (10 -> 8)", stock(pc) == 8, 8, stock(pc))
    bid, nb = (bol.get("id"), bol.get("numeroBoleta")) if isinstance(bol, dict) else (None, None)
    st, mis = req("GET", GW + "/boletas/mis-boletas", token=cli)
    chk("R11", "La boleta aparece en mis-boletas", isinstance(mis, list) and any(x.get("numeroBoleta") == nb for x in mis), "incluida", nb)
    st, _ = req("OPTIONS", GW + "/productos", headers={"Origin": "http://localhost:5173", "Access-Control-Request-Method": "GET"})
    chk("R12", "CORS permite al frontend (localhost:5173)", st == 200, 200, st)

    # ---------- E: verificación de los errores corregidos ----------
    horas = (pl["exp"] - pl["iat"]) / 3600
    chk("E1", "Duración del access token <= 1h", horas <= 1, "<=1h", f"{horas:.0f}h")
    r1 = bcli.get("refreshToken") if isinstance(bcli, dict) else None
    chk("E1", "Login entrega refreshToken y expiresIn", bool(r1) and bcli.get("expiresIn") == 3600, "refreshToken + 3600", bcli.get("expiresIn") if isinstance(bcli, dict) else bcli)
    st, nuevo = req("POST", GW + "/auth/refresh", {"refreshToken": r1})
    r2 = nuevo.get("refreshToken") if isinstance(nuevo, dict) else None
    chk("E1", "POST /auth/refresh entrega un JWT nuevo", st == 200 and r2 and r2 != r1, 200, st)
    st, _ = req("GET", GW + "/boletas/mis-boletas", token=(nuevo or {}).get("token"))
    chk("E1", "El JWT renovado funciona", st == 200, 200, st)
    st, _ = req("POST", GW + "/auth/refresh", {"refreshToken": r1})
    chk("E1", "Reusar un refresh token ya usado -> 401", st == 401, 401, st)
    _, _, b3 = login("alumno@duoc.cl", "1234")
    r3 = b3.get("refreshToken")
    st, _ = req("POST", GW + "/auth/logout", {"refreshToken": r3})
    chk("E1", "POST /auth/logout -> 204", st == 204, 204, st)
    st, _ = req("POST", GW + "/auth/refresh", {"refreshToken": r3})
    chk("E1", "Refresh tras logout -> 401", st == 401, 401, st)
    st, cli, _ = login("alumno@duoc.cl", "1234")

    st, b = comprar(cli, [(1, 1)], {"total": 1, "subtotal": 1})
    tot = b.get("total") if isinstance(b, dict) else b
    chk("E2", "Total lo calcula el servidor (RTX 4090 no cuesta $1)", st == 201 and tot == 1599990, "total=1599990", f"{st} total={tot}")
    st, b = comprar(cli, [(2, 1)], {"total": 100, "descuento": -5000000})
    chk("E2", "Descuento enviado por el cliente se ignora", st == 201 and b.get("descuento") == 0 and b.get("total") == 650000,
        "desc=0 total=650000", f"{st} desc={(b or {}).get('descuento')} total={(b or {}).get('total')}")

    st, _ = req("GET", GW + "/boletas/1", token=cli)
    chk("E3", "CLIENTE alumno NO ve boleta 1 (de pancho)", st == 403, 403, st)
    st, _ = req("GET", GW + "/boletas/numero/BOL-18392044", token=cli)
    chk("E3", "CLIENTE NO busca boletas ajenas por número", st == 403, 403, st)
    st, _ = req("GET", f"{GW}/boletas/{bid}", token=cli)
    chk("E3", "CLIENTE SÍ ve su propia boleta", st == 200, 200, st)
    st, b = req("GET", GW + "/detalles", token=cli)
    chk("E3", "CLIENTE NO lista todos los detalles", st == 403, 403, st)

    email = f"nuevo{U}@test.cl"
    st, b = req("POST", GW + "/auth/register", {"email": email, "password": "Clave123", "nombre": "Nuevo"})
    chk("E4", "Registro devuelve 201 con token", st == 201 and isinstance(b, dict) and b.get("token") and b.get("rol") == "CLIENTE", "201 + token CLIENTE", st)
    st2, _, _ = login(email, "Clave123")
    chk("E4", "El usuario registrado puede iniciar sesión", st2 == 200, 200, st2)
    st, b = req("POST", GW + "/auth/register", {"email": email, "password": "xxxx", "nombre": "N"})
    chk("E4", "Registro con email duplicado -> 409", st == 409, 409, st)
    st, b = req("POST", GW + "/auth/register", {"email": f"esc{U}@test.cl", "password": "xxxx", "nombre": "N", "rol": "ADMIN"})
    chk("E4", "El registro público ignora rol=ADMIN", isinstance(b, dict) and b.get("rol") == "CLIENTE", "CLIENTE", (b or {}).get("rol") if isinstance(b, dict) else b)

    for nombre, url in SVC.items():
        st, b = req("GET", url + "/v3/api-docs")
        chk("E5", f"Puerto de {nombre}-service NO accesible desde fuera", st == 0, "sin conexión", st)
    st, b = req("GET", GW + "/usuarios", token=adm)
    chk("E5", "Respuestas de usuarios sin passwordHash", isinstance(b, list) and all("passwordHash" not in x for x in b), "sin hash", "ok" if isinstance(b, list) else b)

    forjado = forjar_jwt({"sub": "atacante@x.cl", "rol": "ADMIN", "userId": 999, "iat": int(time.time()), "exp": int(time.time()) + 3600}, SECRET_ANTIGUO_REPO)
    st, b = req("GET", GW + "/usuarios", token=forjado)
    chk("E13", "JWT forjado con el secreto antiguo del repo es rechazado", st == 401, 401, st)

    st, p = crear_producto(adm, "A-" + U, 2)
    pa = p["id"]
    st, b = comprar(cli, [(pa, 2), (pa, 1)])
    chk("E8", "Compra rechazada NO descuenta stock (atomicidad)", st == 400 and stock(pa) == 2, "400 y stock=2", f"{st} stock={stock(pa)} '{err(b)[:50]}'")

    st, p = crear_producto(adm, "K-" + U, 10)
    pk = p["id"]
    res = []
    def compra1():
        res.append(comprar(cli, [(pk, 1)])[0])
    hilos = [threading.Thread(target=compra1) for _ in range(20)]
    [h.start() for h in hilos]; [h.join() for h in hilos]
    chk("E9", "20 compras simultáneas de 1 con stock 10: exactamente 10 éxitos", res.count(201) == 10 and stock(pk) == 0,
        "10 x 201, stock 0", f"{res.count(201)} x 201, {res.count(400)} x 400, stock={stock(pk)}")
    st, p = crear_producto(adm, "Q-" + U, 5)
    pq = p["id"]
    res = []
    def compra5():
        res.append(comprar(cli, [(pq, 5)])[0])
    hilos = [threading.Thread(target=compra5) for _ in range(5)]
    [h.start() for h in hilos]; [h.join() for h in hilos]
    chk("E9", "5 compras simultáneas del stock total: sólo 1 éxito", res.count(201) == 1 and stock(pq) == 0, "1 x 201", f"{res.count(201)} x 201, stock={stock(pq)}")

    # E21 antes de modificar detalles
    st, d = req("GET", GW + "/detalles/1", token=adm)
    st, p3 = req("GET", GW + "/productos/3")
    chk("E21", "Detalle semilla coincide con el producto 3", isinstance(d, dict) and d.get("skuProducto") == p3.get("sku"), p3.get("sku"), (d or {}).get("skuProducto"))

    # E10: CRUD completo sobre la boleta creada en R11 (stock de pc = 8)
    st, dets = req("GET", f"{GW}/detalles/boleta/{bid}", token=adm)
    did = dets[0]["id"] if isinstance(dets, list) and dets else None
    st, b = req("PUT", f"{GW}/detalles/{did}", {"idBoleta": bid, "idProducto": pc, "nombreProducto": "Prueba", "cantidad": 2, "precioUnitario": 9000}, adm)
    chk("E10", "PUT /detalles/{id} (ADMIN) recalcula subtotal", st == 200 and (b or {}).get("subtotal") == 18000, "200 subtotal=18000", f"{st} {(b or {}).get('subtotal') if isinstance(b, dict) else b}")
    st, b = req("POST", f"{GW}/detalles", {"idBoleta": bid, "idProducto": pc, "cantidad": 1, "precioUnitario": 1}, adm)
    nd = (b or {}).get("id") if isinstance(b, dict) else None
    chk("E10", "POST /detalles (ADMIN)", st == 201, 201, st)
    st, _ = req("DELETE", f"{GW}/detalles/{nd}", token=adm)
    chk("E10", "DELETE /detalles/{id} (ADMIN)", st == 204, 204, st)
    st, _ = req("DELETE", f"{GW}/boletas/{bid}", token=adm)
    chk("E10", "DELETE de boleta vigente -> 409 (hay que anularla antes)", st == 409, 409, st)
    st, b = req("PUT", f"{GW}/boletas/{bid}", {"estado": "ANULADA"}, adm)
    chk("E10", "PUT /boletas/{id} estado=ANULADA (ADMIN)", st == 200 and (b or {}).get("estado") == "ANULADA", 200, st)
    chk("E10", "Anular repone el stock (8 -> 10)", stock(pc) == 10, 10, stock(pc))
    st, _ = req("PUT", f"{GW}/boletas/{bid}", {"estado": "PAGADA"}, adm)
    chk("E10", "Una boleta anulada no se reactiva -> 409", st == 409, 409, st)
    st, _ = req("DELETE", f"{GW}/boletas/{bid}", token=adm)
    chk("E10", "DELETE /boletas/{id} anulada (ADMIN)", st == 204, 204, st)
    st, _ = req("GET", f"{GW}/detalles/boleta/{bid}", token=adm)

    st, doc = req("GET", "http://localhost:8080/v3/api-docs")
    try:
        sch = json.dumps(doc["paths"]["/api/v1/productos"]["post"]["requestBody"]["content"])
    except Exception as e:
        sch = str(e)
    chk("E11", "Swagger del gateway documenta el cuerpo de POST /productos", "$ref" in sch, "esquema con campos", sch[:90])
    try:
        chk("E11", "Swagger documenta /auth/refresh y /auth/logout", "/api/v1/auth/refresh" in doc["paths"] and "/api/v1/auth/logout" in doc["paths"], "presentes", "ok")
    except Exception as e:
        chk("E11", "Swagger documenta /auth/refresh y /auth/logout", False, "presentes", e)

    casos = [("POST", "/categorias", {"nombre": ""}, adm, "Validación: categoría sin nombre"),
             ("POST", "/productos", {"sku": "X"}, adm, "Validación: producto incompleto"),
             ("POST", "/boletas", {"total": 1, "items": []}, cli, "Validación: boleta sin items"),
             ("POST", "/productos", None, adm, "JSON mal formado")]
    for m, ruta, body, tok, d in casos:
        st, b = req(m, GW + ruta, body, tok, raw="{malo" if body is None else None)
        chk("E12", d + " -> 400", st == 400, 400, f"{st} {err(b)}")
    st, _, b = login("admin@duoc.cl", "incorrecta")
    chk("E12", "Login con clave incorrecta -> 401", st == 401, 401, f"{st} {err(b)}")
    st, _, b = login("noexiste@duoc.cl", "x")
    chk("E12", "Login con email inexistente -> 401 (mismo mensaje)", st == 401, 401, f"{st} {err(b)}")
    st, b = req("GET", GW + "/productos/abc")
    chk("E12", "ID no numérico -> 400", st == 400, 400, st)
    crear_producto(adm, "DUP-" + U, 1)
    st, b = crear_producto(adm, "DUP-" + U, 1)
    chk("E12", "SKU duplicado al crear -> 409", st == 409, 409, st)
    st, b = req("PUT", f"{GW}/productos/{pa}", {"sku": "GPU-RTX-4090FE", "nombre": "x", "precio": 1, "stock": 1, "idCategoria": 1}, adm)
    chk("E12", "SKU duplicado al actualizar -> 409", st == 409, 409, f"{st} {err(b)[:60]}")
    for ruta in ["/auth/no-existe", "/productos/1/no-existe"]:
        st, _ = req("GET", GW + ruta, token=adm)
        chk("E24", f"Ruta inexistente {ruta} -> 404", st == 404, 404, st)
    st, _ = req("PATCH", GW + "/productos/1", {}, adm)
    chk("E24", "Método no soportado -> 405", st == 405, 405, st)

    st, u = req("POST", GW + "/usuarios", {"email": f"inactivo{U}@test.cl", "password": "x1x1", "nombre": "I"}, adm)
    req("PUT", f"{GW}/usuarios/{u['id']}", {"email": f"inactivo{U}@test.cl", "nombre": "I", "activo": False}, adm)
    st, _, b = login(f"inactivo{U}@test.cl", "x1x1")
    chk("E14", "Usuario desactivado NO puede iniciar sesión -> 403", st == 403, 403, f"{st} {err(b)}")

    st, b = crear_producto(adm, "NC-" + U, 1, cat=9999)
    chk("E16", "Rechaza producto con categoría inexistente", st == 400, 400, f"{st} {err(b)}")
    st, c = req("POST", GW + "/categorias", {"nombre": "Borrable " + U}, adm)
    crear_producto(adm, "CB-" + U, 1, cat=c["id"])
    st, b = req("DELETE", f"{GW}/categorias/{c['id']}", token=adm)
    chk("E16", "No borra categoría que tiene productos -> 409", st == 409, 409, f"{st} {err(b)[:60]}")
    st, p = crear_producto(adm, "IN-" + U, 3, activo=False)
    st, b = comprar(cli, [(p["id"], 1)])
    chk("E16", "No permite comprar producto inactivo -> 400", st == 400, 400, f"{st} {err(b)}")
    st, _ = req("PUT", GW + "/categorias/1", {"nombre": "GPUs " + U}, adm)
    st, b = req("GET", GW + "/productos/1")
    chk("E16", "Renombrar categoría actualiza nombreCategoria en productos", isinstance(b, dict) and b.get("nombreCategoria") == "GPUs " + U, "GPUs ...", (b or {}).get("nombreCategoria"))
    req("PUT", GW + "/categorias/1", {"nombre": "Tarjetas Gráficas"}, adm)

    st, b = req("POST", GW + "/usuarios", {"email": f"sinclave{U}@test.cl", "nombre": "S"}, adm)
    chk("E17", "Crear usuario sin contraseña -> 400", st == 400, 400, f"{st} {err(b)}")
    st, b = req("POST", GW + "/usuarios", {"email": f"rol{U}@test.cl", "nombre": "S", "password": "xxxx", "rol": "SUPERADMIN"}, adm)
    chk("E17", "Rol inexistente -> 400", st == 400, 400, f"{st} {err(b)[:50]}")

    st, _ = req("GET", "http://localhost:8080/h2-console/")
    chk("E22", "Consola H2 del gateway NO expuesta", st not in (200, 302), "401/404", st)

    t0 = time.time(); st, bs = req("GET", GW + "/boletas", token=adm); dt = (time.time() - t0) * 1000
    chk("E19", "Listado de boletas en 1 llamada a detalle-service", st == 200 and all(x["detalles"] for x in bs), "todas con detalle", f"{len(bs)} boletas, {dt:.0f} ms")

    # ---------- F2: cupones validados y calculados en el servidor ----------
    st, b = crear_producto(adm, "CUP-" + U, 10, precio=99999)
    pcup = (b or {}).get("id")
    st, b = req("GET", GW + "/cupones/validar/hardware2026", token=cli)
    chk("F2", "Validar cupón semilla (minúsculas) como CLIENTE", st == 200 and (b or {}).get("porcentaje") == 15, "200 15%", f"{st} {b}")
    st, b = req("GET", GW + "/cupones/validar/HARDWARE2026")
    chk("F2", "Validar cupón sin sesión -> 401", st == 401, 401, st)
    st, b = comprar(cli, [(pcup, 2)], {"codigoCupon": "HARDWARE2026", "descuento": 199998, "total": 1})
    ok = isinstance(b, dict) and b.get("subtotal") == 199998 and b.get("descuento") == 30000 and b.get("total") == 169998 and b.get("codigoCupon") == "HARDWARE2026"
    chk("F2", "Compra con cupón: descuento 15% calculado en servidor (ignora montos del cliente)", st == 201 and ok, "201 199998-30000=169998", f"{st} {b.get('subtotal') if isinstance(b, dict) else b}/{b.get('descuento') if isinstance(b, dict) else ''}/{b.get('total') if isinstance(b, dict) else ''}")
    s0 = stock(pcup)
    st, b = comprar(cli, [(pcup, 1)], {"codigoCupon": "NOEXISTE"})
    chk("F2", "Cupón inexistente -> 400 y stock intacto", st == 400 and stock(pcup) == s0, f"400 stock {s0}", f"{st} {err(b)} stock {stock(pcup)}")
    st, b = req("POST", GW + "/cupones", {"codigo": "e2e" + U, "porcentaje": 20}, adm)
    idc = (b or {}).get("id") if isinstance(b, dict) else None
    chk("F2", "ADMIN crea cupón (se guarda en mayúsculas)", st == 201 and (b or {}).get("codigo") == "E2E" + U, 201, f"{st} {b}")
    req("PUT", f"{GW}/cupones/{idc}", {"codigo": "E2E" + U, "porcentaje": 20, "activo": False}, adm)
    st, b = comprar(cli, [(pcup, 1)], {"codigoCupon": "E2E" + U})
    chk("F2", "Cupón desactivado -> 400 al pagar", st == 400 and stock(pcup) == s0, 400, f"{st} {err(b)}")
    st, _ = req("GET", GW + "/cupones", token=cli)
    st2, _ = req("POST", GW + "/cupones", {"codigo": "HACK" + U, "porcentaje": 90}, cli)
    chk("F2", "CLIENTE no lista ni crea cupones -> 403", st == 403 and st2 == 403, "403/403", f"{st}/{st2}")
    st, b = req("POST", GW + "/cupones", {"codigo": "HARDWARE2026", "porcentaje": 5}, adm)
    chk("F2", "Cupón duplicado -> 409", st == 409, 409, st)
    st, b = req("POST", GW + "/cupones", {"codigo": "x", "porcentaje": 95}, adm)
    chk("F2", "Cupón inválido -> 400 con detalles", st == 400 and isinstance(b, dict) and "detalles" in b, 400, f"{st} {b}")
    st, _ = req("DELETE", f"{GW}/cupones/{idc}", token=adm)
    chk("F2", "ADMIN elimina cupón -> 204", st == 204, 204, st)

    with open("estado.json", "w") as f:
        json.dump({"adm": adm, "cli": cli, "pc": pc, "U": U}, f)


def caida():
    est = json.load(open("estado.json"))
    adm, cli = est["adm"], est["cli"]
    st, p = crear_producto(adm, "D-" + est["U"], 5)
    pd = p["id"]
    st, b = comprar(cli, [(pd, 2)])
    chk("E8", "Con detalle-service caído la compra NO se confirma (503)", st == 503, 503, f"{st} {err(b)[:60]}")
    chk("E8", "...y el stock se repone por compensación (5)", stock(pd) == 5, 5, stock(pd))
    st, b = req("GET", GW + "/detalles", token=adm)
    chk("E12", "Servicio caído -> 503 sin detalles internos", st == 503 and "http" not in str(b), "503 genérico", f"{st} {err(b)}")


def tras_caida():
    est = json.load(open("estado.json"))
    st, bs = req("GET", GW + "/boletas", token=est["adm"])
    sin = [x["numeroBoleta"] for x in bs if not x["detalles"]] if isinstance(bs, list) else bs
    chk("E8", "No existen boletas sin detalles", st == 200 and sin == [], "0", f"{st} {sin}")


def reinicio():
    est = json.load(open("estado.json"))
    st, _ = req("GET", f"{GW}/productos/{est['pc']}")
    chk("E6", "Producto creado sobrevive al reinicio (MySQL)", st == 200, 200, st)
    st, _, _ = login(f"nuevo{est['U']}@test.cl", "Clave123")
    chk("E6", "Usuario registrado sobrevive al reinicio", st == 200, 200, st)
    st, bs = req("GET", GW + "/boletas", token=login("admin@duoc.cl", "admin")[1])
    sin = [x["numeroBoleta"] for x in bs if not x["detalles"]] if isinstance(bs, list) else bs
    chk("E6", "Boletas conservan sus detalles tras el reinicio", st == 200 and sin == [], "0 sin detalle", f"{len(bs) if isinstance(bs, list) else bs} boletas, {len(sin) if isinstance(sin, list) else sin} sin detalle")


if __name__ == "__main__":
    fase = sys.argv[1] if len(sys.argv) > 1 else "principal"
    {"principal": principal, "caida": caida, "tras_caida": tras_caida, "reinicio": reinicio}[fase]()
    for r in RES:
        print(" | ".join(r))
    print(f"\nTOTAL {len(RES)} | OK {sum(r[1] == 'OK ' for r in RES)} | FALLA {sum(r[1] == 'FALLA' for r in RES)}")
