package com.mayle.ortopedia

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mayle.ortopedia.ui.theme.MayLeOrtopediaTheme
import java.util.Locale

data class Variante(
    val nombre: String = "",
    val stock: Long = 0
)

data class Producto(
    val id: String = "",
    val codigo: String = "",
    val nombre: String = "",
    val precioContado: Double = 0.0,
    val precioUnPago: Double = 0.0,
    val precioTresCuotas: Double = 0.0,
    val stockPorPuntoVenta: Map<String, List<Variante>> = emptyMap(),
    val variantesDefault: List<Variante> = emptyList()
) {
    fun obtenerVariantesParaPuntoVenta(puntoVenta: String): List<Variante> {
        val pvNormalizado = puntoVenta.trim().uppercase()
        val variantesPV = stockPorPuntoVenta[pvNormalizado]
            ?: stockPorPuntoVenta[puntoVenta]

        if (variantesPV != null && variantesPV.isNotEmpty()) {
            return variantesPV
        }

        return variantesDefault
    }
}

data class VentaItem(
    val producto: Producto,
    val variante: String,
    val cantidad: Long,
    val precioUnitario: Double,
    val formaPago: String
)

data class UsuarioApp(
    val id: String = "",
    val email: String = "",
    val role: String = "vendedor",
    val puntoVenta: String = "",
    val activo: Boolean = true
)

data class VarianteEntrada(
    val nombre: String = "",
    val stock: String = "0"
)

fun formatoPrecio(valor: Double): String {
    return if (valor % 1.0 == 0.0) {
        String.format(
            Locale.US,
            "%.0f",
            valor
        )
    } else {
        String.format(
            Locale.US,
            "%.2f",
            valor
        )
            .trimEnd('0')
            .trimEnd('.')
    }
}

fun precioSegunPago(
    producto: Producto,
    formaPago: String
): Double {
    return when (formaPago) {
        "EFECTIVO" ->
            producto.precioContado

        "UN_PAGO" ->
            producto.precioUnPago

        "TRES_CUOTAS" ->
            producto.precioTresCuotas

        else ->
            producto.precioUnPago
    }
}

fun variantesDesdeDocumento(
    documento: DocumentSnapshot
): List<Variante> {
    val variantesFirebase = documento.get("variantes")

    if (variantesFirebase is Map<*, *>) {
        return variantesFirebase
            .mapNotNull { entrada ->
                val nombre = entrada.key?.toString() ?: return@mapNotNull null
                val stock = when (val valor = entrada.value) {
                    is Number -> valor.toLong()
                    else -> valor?.toString()?.toLongOrNull() ?: 0L
                }
                Variante(nombre = nombre, stock = stock)
            }
            .sortedBy { it.nombre.lowercase() }
    }

    val resultado = mutableListOf<Variante>()
    documento.getLong("stockTalle1")?.let { resultado.add(Variante("1", it)) }
    documento.getLong("stockTalle2")?.let { resultado.add(Variante("2", it)) }
    documento.getLong("stockTalle3")?.let { resultado.add(Variante("3", it)) }

    return resultado
}

fun productoDesdeDocumento(
    documento: DocumentSnapshot
): Producto {
    val stockPorPv = mutableMapOf<String, List<Variante>>()

    val pvData = documento.get("puntosVenta") ?: documento.get("stockPorPuntoVenta")
    if (pvData is Map<*, *>) {
        pvData.forEach { (pvKey, pvVal) ->
            val pvNombre = pvKey?.toString()?.trim()?.uppercase() ?: return@forEach
            if (pvVal is Map<*, *>) {
                val lista = pvVal.mapNotNull { (vKey, vVal) ->
                    val name = vKey?.toString() ?: return@mapNotNull null
                    val st = when (vVal) {
                        is Number -> vVal.toLong()
                        else -> vVal?.toString()?.toLongOrNull() ?: 0L
                    }
                    Variante(nombre = name, stock = st)
                }.sortedBy { it.nombre.lowercase() }
                stockPorPv[pvNombre] = lista
            }
        }
    }

    val defaultVars = variantesDesdeDocumento(documento)

    return Producto(
        id = documento.id,
        codigo = documento.getString("codigo") ?: "",
        nombre = documento.getString("nombre") ?: "",
        precioContado = documento.getDouble("precioContado") ?: 0.0,
        precioUnPago = documento.getDouble("precioUnPago") ?: 0.0,
        precioTresCuotas = documento.getDouble("precioTresCuotas") ?: 0.0,
        stockPorPuntoVenta = stockPorPv,
        variantesDefault = defaultVars
    )
}

fun usuarioDesdeDocumento(
    documento: DocumentSnapshot
): UsuarioApp {
    return UsuarioApp(
        id = documento.id,
        email = documento.getString("email") ?: "",
        role = documento.getString("role") ?: "vendedor",
        puntoVenta = documento.getString("puntoVenta") ?: "",
        activo = documento.getBoolean("activo") ?: true
    )
}

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            MayLeOrtopediaTheme {
                MayLeApp()
            }
        }
    }
}

@Composable
fun MayLeApp() {
    var usuarioIngresado by remember {
        mutableStateOf(
            FirebaseAuth.getInstance().currentUser != null
        )
    }

    if (usuarioIngresado) {
        MayLePrincipal(
            onCerrarSesion = {
                FirebaseAuth.getInstance().signOut()
                usuarioIngresado = false
            }
        )
    } else {
        MayLeLogin(
            onLoginCorrecto = {
                usuarioIngresado = true
            }
        )
    }
}

@Composable
fun MayLeLogin(
    onLoginCorrecto: () -> Unit
) {
    val azulMayLe = Color(0xFF123B5D)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mostrarPassword by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var ingresando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MayLe Ortopedia",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(50.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                mensaje = ""
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                mensaje = ""
            },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (mostrarPassword) {
                androidx.compose.ui.text.input.VisualTransformation.None
            } else {
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(
                    onClick = { mostrarPassword = !mostrarPassword }
                ) {
                    Text(
                        text = if (mostrarPassword) "OCULTAR" else "MOSTRAR",
                        color = azulMayLe
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    mensaje = "Complete email y contraseña"
                    return@Button
                }

                ingresando = true
                mensaje = ""

                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { tarea ->
                        if (!tarea.isSuccessful) {
                            ingresando = false
                            mensaje = "Email o contraseña incorrectos"
                            return@addOnCompleteListener
                        }

                        val usuario = FirebaseAuth.getInstance().currentUser
                        if (usuario == null) {
                            ingresando = false
                            mensaje = "No se pudo validar el usuario"
                            return@addOnCompleteListener
                        }

                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(usuario.uid)
                            .get()
                            .addOnSuccessListener { documento ->
                                val activo = documento.getBoolean("activo") ?: true
                                if (!activo) {
                                    FirebaseAuth.getInstance().signOut()
                                    ingresando = false
                                    mensaje = "Este usuario está desactivado"
                                } else {
                                    ingresando = false
                                    onLoginCorrecto()
                                }
                            }
                            .addOnFailureListener {
                                ingresando = false
                                mensaje = "No se pudo validar el usuario"
                            }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (ingresando) "INGRESANDO..." else "INGRESAR",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (mensaje.isNotEmpty()) {
            Text(
                text = mensaje,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MayLePrincipal(
    onCerrarSesion: () -> Unit
) {
    val azulMayLe = Color(0xFF123B5D)

    var rol by remember { mutableStateOf("") }
    var puntoVenta by remember { mutableStateOf("") }
    var pantalla by remember { mutableStateOf("menu") }

    val usuario = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(usuario?.uid) {
        if (usuario != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(usuario.uid)
                .get()
                .addOnSuccessListener { documento ->
                    rol = documento.getString("role") ?: ""
                    puntoVenta = documento.getString("puntoVenta") ?: ""
                }
        }
    }

    when (pantalla) {
        "ventas" -> {
            MayLeVentas(
                puntoVenta = if (rol == "admin" || puntoVenta.isBlank()) "SIN ASIGNAR" else puntoVenta,
                onVolver = { pantalla = "menu" }
            )
        }

        "transferencias" -> {
            MayLeTransferencias(
                puntoVentaUsuario = if (rol == "admin" || puntoVenta.isBlank()) "SIN ASIGNAR" else puntoVenta,
                esAdmin = rol == "admin",
                onVolver = { pantalla = "menu" }
            )
        }

        "productos" -> {
            MayLeProductosStock(
                esAdmin = rol == "admin",
                onVolver = { pantalla = "menu" }
            )
        }

        "administracion" -> {
            MayLeAdministracion(
                onVolver = { pantalla = "menu" }
            )
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "MayLe Ortopedia",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = azulMayLe
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (rol == "admin") "Administrador" else "Vendedor",
                    fontSize = 18.sp,
                    color = azulMayLe
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Punto de venta: " + if (rol == "admin" || puntoVenta.isBlank()) "SIN ASIGNAR" else puntoVenta,
                    fontSize = 16.sp,
                    color = azulMayLe
                )

                Spacer(modifier = Modifier.height(30.dp))

                MayLeModuloButton(texto = "VENTAS") {
                    pantalla = "ventas"
                }

                Spacer(modifier = Modifier.height(14.dp))

                MayLeModuloButton(texto = "PRODUCTOS Y STOCK") {
                    pantalla = "productos"
                }

                Spacer(modifier = Modifier.height(14.dp))

                MayLeModuloButton(texto = "TRANSFERENCIAS DE STOCK") {
                    pantalla = "transferencias"
                }

                Spacer(modifier = Modifier.height(14.dp))

                MayLeModuloButton(texto = "CAJA") {}

                Spacer(modifier = Modifier.height(14.dp))

                MayLeModuloButton(texto = "COMISIONES") {}

                if (rol == "admin") {
                    Spacer(modifier = Modifier.height(14.dp))

                    MayLeModuloButton(texto = "ADMINISTRACIÓN") {
                        pantalla = "administracion"
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    MayLeModuloButton(texto = "REPORTES") {}
                }

                Spacer(modifier = Modifier.height(30.dp))

                TextButton(onClick = onCerrarSesion) {
                    Text(
                        text = "CERRAR SESIÓN",
                        color = azulMayLe,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MayLeAdministracion(
    onVolver: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    var pantalla by remember { mutableStateOf("menu") }

    if (pantalla == "usuarios") {
        MayLeUsuarios(onVolver = { pantalla = "menu" })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        MayLeVolverButton(onClick = onVolver)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Administración",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF123B5D)
        )

        Spacer(modifier = Modifier.height(30.dp))

        MayLeModuloButton(texto = "USUARIOS Y PUNTOS DE VENTA") {
            pantalla = "usuarios"
        }
    }
}

@Composable
fun MayLeUsuarios(
    onVolver: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)

    var usuarios by remember { mutableStateOf(listOf<UsuarioApp>()) }
    var cargando by remember { mutableStateOf(true) }
    var mostrandoFormulario by remember { mutableStateOf(false) }
    var usuarioEditando by remember { mutableStateOf<UsuarioApp?>(null) }
    var mensaje by remember { mutableStateOf("") }

    fun cargarUsuarios() {
        cargando = true

        FirebaseFirestore.getInstance()
            .collection("users")
            .get()
            .addOnSuccessListener { resultado ->
                usuarios = resultado.documents
                    .map { usuarioDesdeDocumento(it) }
                    .sortedBy { it.email.lowercase() }
                cargando = false
            }
            .addOnFailureListener {
                cargando = false
                mensaje = "No se pudieron cargar los usuarios"
            }
    }

    LaunchedEffect(Unit) {
        cargarUsuarios()
    }

    if (mostrandoFormulario) {
        MayLeNuevoUsuario(
            onVolver = { mostrandoFormulario = false },
            onUsuarioGuardado = {
                mostrandoFormulario = false
                cargarUsuarios()
            }
        )
        return
    }

    if (usuarioEditando != null) {
        MayLeEditarUsuario(
            usuario = usuarioEditando!!,
            onVolver = { usuarioEditando = null },
            onUsuarioGuardado = {
                usuarioEditando = null
                cargarUsuarios()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        MayLeVolverButton(onClick = onVolver)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Usuarios",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { mostrandoFormulario = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("NUEVO USUARIO")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (cargando) {
            Text("Cargando usuarios...")
        } else if (usuarios.isEmpty()) {
            Text("Todavía no hay usuarios cargados.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(usuarios) { usuario ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = usuario.email,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Rol: " + if (usuario.role == "admin") "Administrador" else "Vendedor"
                            )

                            Text(
                                text = "Punto de venta: " + if (usuario.puntoVenta.isBlank()) "SIN ASIGNAR" else usuario.puntoVenta
                            )

                            Text(
                                text = "Estado: " + if (usuario.activo) "ACTIVO" else "INACTIVO",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { usuarioEditando = usuario },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("EDITAR")
                            }
                        }
                    }
                }
            }
        }

        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = mensaje,
                color = Color.Red
            )
        }
    }
}

@Composable
fun MayLeNuevoUsuario(
    onVolver: () -> Unit,
    onUsuarioGuardado: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)
    val contexto = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var puntoVenta by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf("vendedor") }
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        MayLeVolverButton(onClick = onVolver)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Nuevo usuario",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña inicial") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = puntoVenta,
            onValueChange = { puntoVenta = it.take(10).uppercase() },
            label = { Text("Punto de venta") },
            supportingText = { Text("${puntoVenta.length}/10") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Rol",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { rol = "vendedor" },
                modifier = Modifier.weight(1f)
            ) {
                Text("VENDEDOR")
            }

            Button(
                onClick = { rol = "admin" },
                modifier = Modifier.weight(1f)
            ) {
                Text("ADMIN")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank()) {
                    mensaje = "Ingrese el email"
                    return@Button
                }

                if (password.length < 6) {
                    mensaje = "La contraseña debe tener al menos 6 caracteres"
                    return@Button
                }

                if (rol == "vendedor" && puntoVenta.isBlank()) {
                    mensaje = "Ingrese el punto de venta para el vendedor"
                    return@Button
                }

                guardando = true
                mensaje = ""

                crearUsuarioFirebase(
                    contexto = contexto,
                    email = email.trim(),
                    password = password,
                    rol = rol,
                    puntoVenta = if (rol == "admin") "" else puntoVenta.trim().take(10).uppercase(),
                    onSuccess = {
                        guardando = false
                        onUsuarioGuardado()
                    },
                    onError = { error ->
                        guardando = false
                        mensaje = error
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !guardando
        ) {
            Text(
                text = if (guardando) "CREANDO..." else "CREAR USUARIO",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "El nuevo usuario quedará activo y podrá iniciar sesión con este email y contraseña.",
            fontSize = 14.sp,
            color = Color.Gray
        )

        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensaje,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun crearUsuarioFirebase(
    contexto: Context,
    email: String,
    password: String,
    rol: String,
    puntoVenta: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val nombreApp = "MayLeSecondaryAuth"

    val appSecundaria = try {
        FirebaseApp.getInstance(nombreApp)
    } catch (exception: IllegalStateException) {
        FirebaseApp.initializeApp(
            contexto,
            FirebaseApp.getInstance().options,
            nombreApp
        )
    }

    if (appSecundaria == null) {
        onError("No se pudo preparar Firebase")
        return
    }

    val authSecundario = FirebaseAuth.getInstance(appSecundaria)

    authSecundario.createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener { resultado ->
            val nuevoUsuario = resultado.user
            if (nuevoUsuario == null) {
                authSecundario.signOut()
                onError("No se pudo obtener el nuevo usuario")
                return@addOnSuccessListener
            }

            val datos = hashMapOf(
                "email" to email,
                "role" to rol,
                "puntoVenta" to puntoVenta,
                "activo" to true
            )

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(nuevoUsuario.uid)
                .set(datos)
                .addOnSuccessListener {
                    authSecundario.signOut()
                    onSuccess()
                }
                .addOnFailureListener {
                    authSecundario.signOut()
                    onError("Se creó el usuario pero no se pudo guardar su perfil")
                }
        }
        .addOnFailureListener { error ->
            onError(error.message ?: "No se pudo crear el usuario")
        }
}

@Composable
fun MayLeEditarUsuario(
    usuario: UsuarioApp,
    onVolver: () -> Unit,
    onUsuarioGuardado: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)

    var rol by remember { mutableStateOf(usuario.role) }
    var puntoVenta by remember { mutableStateOf(usuario.puntoVenta) }
    var activo by remember { mutableStateOf(usuario.activo) }
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        MayLeVolverButton(onClick = onVolver)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Editar usuario",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = usuario.email,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = puntoVenta,
            onValueChange = { puntoVenta = it.take(10).uppercase() },
            label = { Text("Punto de venta") },
            supportingText = { Text("${puntoVenta.length}/10") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Rol",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { rol = "vendedor" },
                modifier = Modifier.weight(1f)
            ) {
                Text("VENDEDOR")
            }

            Button(
                onClick = { rol = "admin" },
                modifier = Modifier.weight(1f)
            ) {
                Text("ADMIN")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Estado",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { activo = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("ACTIVO")
            }

            Button(
                onClick = { activo = false },
                modifier = Modifier.weight(1f)
            ) {
                Text("INACTIVO")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (rol == "vendedor" && puntoVenta.isBlank()) {
                    mensaje = "Ingrese el punto de venta para el vendedor"
                    return@Button
                }

                guardando = true
                mensaje = ""

                val pvLimpio = if (rol == "admin") "" else puntoVenta.trim().take(10).uppercase()

                val cambios = hashMapOf<String, Any>(
                    "role" to rol,
                    "puntoVenta" to pvLimpio,
                    "activo" to activo
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(usuario.id)
                    .update(cambios)
                    .addOnSuccessListener {
                        guardando = false
                        onUsuarioGuardado()
                    }
                    .addOnFailureListener {
                        guardando = false
                        mensaje = "No se pudo guardar el usuario"
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !guardando
        ) {
            Text(
                text = if (guardando) "GUARDANDO..." else "GUARDAR CAMBIOS",
                fontWeight = FontWeight.Bold
            )
        }

        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensaje,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MayLeVentas(
    puntoVenta: String,
    onVolver: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)
    val contexto = LocalContext.current

    var productos by remember { mutableStateOf(listOf<Producto>()) }
    var buscando by remember { mutableStateOf("") }
    var productoSeleccionado by remember { mutableStateOf<Producto?>(null) }
    var varianteSeleccionada by remember { mutableStateOf<Variante?>(null) }
    var cantidad by remember { mutableStateOf("1") }
    var formaPago by remember { mutableStateOf("UN_PAGO") }
    var nombreComprador by remember { mutableStateOf("") }
    var carrito by remember { mutableStateOf(listOf<VentaItem>()) }
    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var comprobante by remember { mutableStateOf("") }

    fun cargarProductos() {
        cargando = true
        FirebaseFirestore.getInstance()
            .collection("products")
            .get()
            .addOnSuccessListener { resultado ->
                productos = resultado.documents.map { productoDesdeDocumento(it) }
                cargando = false
            }
            .addOnFailureListener {
                cargando = false
                mensaje = "No se pudieron cargar los productos"
            }
    }

    LaunchedEffect(Unit) {
        cargarProductos()
    }

    val productosFiltrados = productos.filter { producto ->
        val texto = buscando.trim().lowercase()
        texto.isEmpty() ||
                producto.codigo.lowercase().contains(texto) ||
                producto.nombre.lowercase().contains(texto)
    }

    val precioActual = productoSeleccionado?.let {
        precioSegunPago(it, formaPago)
    } ?: 0.0

    val cantidadActual = cantidad.toLongOrNull() ?: 0L

    val totalCarrito = carrito.sumOf {
        it.precioUnitario * it.cantidad
    }

    if (comprobante.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            MayLeVolverButton(onClick = onVolver)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Venta realizada",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = comprobante,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, comprobante)
                    }
                    contexto.startActivity(
                        Intent.createChooser(intent, "Compartir comprobante")
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("COMPARTIR COMPROBANTE")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    comprobante = ""
                    carrito = emptyList()
                    nombreComprador = ""
                    productoSeleccionado = null
                    varianteSeleccionada = null
                    buscando = ""
                    mensaje = ""
                    cargarProductos()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("NUEVA VENTA")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        MayLeVolverButton(onClick = onVolver)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "VENTAS",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Punto de venta: $puntoVenta",
            color = azulMayLe,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = buscando,
            onValueChange = {
                buscando = it
                productoSeleccionado = null
                varianteSeleccionada = null
            },
            label = { Text("Buscar por código o nombre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (cargando) {
            Text("Cargando productos...")
        } else if (productoSeleccionado == null) {
            if (productosFiltrados.isEmpty()) {
                Text("No se encontraron productos.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(productosFiltrados) { producto ->
                        OutlinedButton(
                            onClick = {
                                productoSeleccionado = producto
                                buscando = producto.nombre
                                varianteSeleccionada = null
                                mensaje = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = producto.nombre,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Código: " + producto.codigo
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val producto = productoSeleccionado!!
            val variantesDisponibles = producto.obtenerVariantesParaPuntoVenta(puntoVenta)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = producto.nombre,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(text = "Código: " + producto.codigo)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Efectivo: $" + formatoPrecio(producto.precioContado))
                    Text(text = "1 pago: $" + formatoPrecio(producto.precioUnPago))
                    Text(text = "3 cuotas: $" + formatoPrecio(producto.precioTresCuotas))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Seleccione variante",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (variantesDisponibles.isEmpty()) {
                Text("Este producto no tiene variantes.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(150.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(variantesDisponibles) { variante ->
                        OutlinedButton(
                            onClick = {
                                varianteSeleccionada = variante
                                mensaje = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "${variante.nombre} | Stock: ${variante.stock}")
                        }
                    }
                }
            }

            if (varianteSeleccionada != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Variante: " + varianteSeleccionada!!.nombre,
                    fontWeight = FontWeight.Bold,
                    color = azulMayLe
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Forma de pago",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { formaPago = "EFECTIVO" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("EFECTIVO")
                }

                Button(
                    onClick = { formaPago = "UN_PAGO" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("1 PAGO")
                }

                Button(
                    onClick = { formaPago = "TRES_CUOTAS" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("3 CUOTAS")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Precio seleccionado: $" + formatoPrecio(precioActual),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = cantidad,
                onValueChange = {
                    cantidad = it.filter { caracter -> caracter.isDigit() }
                },
                label = { Text("Cantidad") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (varianteSeleccionada == null) {
                        mensaje = "Seleccione una variante"
                        return@Button
                    }

                    if (cantidadActual <= 0) {
                        mensaje = "La cantidad debe ser mayor a cero"
                        return@Button
                    }

                    if (carrito.size >= 10) {
                        mensaje = "El carrito permite hasta 10 líneas diferentes"
                        return@Button
                    }

                    val nuevoItem = VentaItem(
                        producto = producto,
                        variante = varianteSeleccionada!!.nombre,
                        cantidad = cantidadActual,
                        precioUnitario = precioActual,
                        formaPago = formaPago
                    )

                    val existente = carrito.indexOfFirst {
                        it.producto.id == producto.id &&
                                it.variante == varianteSeleccionada!!.nombre &&
                                it.formaPago == formaPago
                    }

                    carrito = if (existente >= 0) {
                        carrito.mapIndexed { indice, item ->
                            if (indice == existente) {
                                item.copy(cantidad = item.cantidad + cantidadActual)
                            } else {
                                item
                            }
                        }
                    } else {
                        carrito + nuevoItem
                    }

                    productoSeleccionado = null
                    varianteSeleccionada = null
                    buscando = ""
                    cantidad = "1"
                    mensaje = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("AGREGAR AL CARRITO")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (carrito.isNotEmpty()) {
            Text(
                text = "CARRITO (${carrito.size}/10)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.height(190.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(carrito) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = item.producto.nombre,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${item.variante} × ${item.cantidad}"
                            )

                            Text(
                                text = "Pago: " + when (item.formaPago) {
                                    "EFECTIVO" -> "Efectivo"
                                    "UN_PAGO" -> "1 pago"
                                    else -> "3 cuotas"
                                }
                            )

                            Text(
                                text = "Subtotal: $" + formatoPrecio(item.precioUnitario * item.cantidad)
                            )

                            TextButton(
                                onClick = {
                                    carrito = carrito.filterNot {
                                        it.producto.id == item.producto.id &&
                                                it.variante == item.variante &&
                                                it.formaPago == item.formaPago
                                    }
                                }
                            ) {
                                Text("ELIMINAR")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = nombreComprador,
                onValueChange = { nombreComprador = it },
                label = { Text("Nombre del comprador (Obligatorio)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "TOTAL: $" + formatoPrecio(totalCarrito),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (nombreComprador.isBlank()) {
                        mensaje = "El nombre del comprador es obligatorio"
                        return@Button
                    }

                    guardando = true
                    mensaje = ""

                    val db = FirebaseFirestore.getInstance()
                    val usuario = FirebaseAuth.getInstance().currentUser

                    if (usuario == null) {
                        guardando = false
                        mensaje = "La sesión no es válida"
                        return@Button
                    }

                    val referenciaVenta = db.collection("ventas").document()

                    db.runTransaction { transaction ->
                        val pvActualKey = puntoVenta.trim().uppercase()

                        for (item in carrito) {
                            val referenciaProducto = db.collection("products").document(item.producto.id)
                            val documento = transaction.get(referenciaProducto)

                            val pvData = documento.get("puntosVenta") as? Map<*, *>
                            val mapaPuntosVenta = mutableMapOf<String, MutableMap<String, Any>>()

                            if (pvData != null) {
                                pvData.forEach { (k, v) ->
                                    val pvName = k?.toString()?.trim()?.uppercase() ?: return@forEach
                                    if (v is Map<*, *>) {
                                        val varMap = mutableMapOf<String, Any>()
                                        v.forEach { (vk, vv) ->
                                            if (vk != null) {
                                                val st = when (vv) {
                                                    is Number -> vv.toLong()
                                                    else -> vv?.toString()?.toLongOrNull() ?: 0L
                                                }
                                                varMap[vk.toString()] = st
                                            }
                                        }
                                        mapaPuntosVenta[pvName] = varMap
                                    }
                                }
                            }

                            val mapaVariantesPV = mapaPuntosVenta.getOrPut(pvActualKey) {
                                val seed = mutableMapOf<String, Any>()
                                variantesDesdeDocumento(documento).forEach { seed[it.nombre] = it.stock }
                                seed
                            }

                            val stockActual = (mapaVariantesPV[item.variante] as? Number)?.toLong() ?: 0L
                            mapaVariantesPV[item.variante] = stockActual - item.cantidad

                            transaction.update(referenciaProducto, "puntosVenta", mapaPuntosVenta)
                        }

                        val itemsVenta = carrito.map { item ->
                            hashMapOf(
                                "productoId" to item.producto.id,
                                "codigo" to item.producto.codigo,
                                "nombre" to item.producto.nombre,
                                "variante" to item.variante,
                                "cantidad" to item.cantidad,
                                "precioUnitario" to item.precioUnitario,
                                "subtotal" to item.precioUnitario * item.cantidad,
                                "formaPago" to item.formaPago
                            )
                        }

                        val comisionCalculada = totalCarrito * 0.10

                        val datosVenta = hashMapOf(
                            "fechaHora" to FieldValue.serverTimestamp(),
                            "usuarioId" to usuario.uid,
                            "usuarioEmail" to (usuario.email ?: ""),
                            "puntoVenta" to puntoVenta.take(10).uppercase(),
                            "nombreComprador" to nombreComprador.trim(),
                            "formaPago" to formaPago,
                            "total" to totalCarrito,
                            "comision" to comisionCalculada,
                            "items" to itemsVenta,
                            "estado" to "confirmada"
                        )

                        transaction.set(referenciaVenta, datosVenta)
                        null
                    }
                        .addOnSuccessListener {
                            guardando = false
                            val numeroVenta = referenciaVenta.id.takeLast(6)

                            comprobante = buildString {
                                appendLine("MAYLE ORTOPEDIA")
                                appendLine("COMPROBANTE DE VENTA")
                                appendLine()
                                appendLine("Venta: $numeroVenta")
                                appendLine("Punto de venta: $puntoVenta")
                                appendLine("Comprador: ${nombreComprador.trim()}")
                                appendLine()
                                carrito.forEach { item ->
                                    appendLine(item.producto.nombre)
                                    appendLine("Código: ${item.producto.codigo}")
                                    appendLine("Variante: ${item.variante}")
                                    appendLine("Cantidad: ${item.cantidad}")
                                    appendLine("Subtotal: $" + formatoPrecio(item.precioUnitario * item.cantidad))
                                    appendLine()
                                }
                                appendLine("TOTAL: $" + formatoPrecio(totalCarrito))
                                appendLine("Comisión generada: $" + formatoPrecio(totalCarrito * 0.10))
                            }
                        }
                        .addOnFailureListener {
                            guardando = false
                            mensaje = "No se pudo registrar la venta"
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !guardando
            ) {
                Text(
                    text = if (guardando) "REGISTRANDO VENTA..." else "CONFIRMAR VENTA",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = mensaje,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MayLeTransferencias(
    puntoVentaUsuario: String,
    esAdmin: Boolean,
    onVolver: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)
    val db = FirebaseFirestore.getInstance()
    val usuario = FirebaseAuth.getInstance().currentUser

    var productos by remember { mutableStateOf(listOf<Producto>()) }

    var origen by remember {
        mutableStateOf(
            if (esAdmin) (if (puntoVentaUsuario.isNotBlank() && puntoVentaUsuario != "SIN ASIGNAR") puntoVentaUsuario else "SANTIAGO")
            else puntoVentaUsuario
        )
    }

    var destino by remember { mutableStateOf("") }
    var buscando by remember { mutableStateOf("") }
    var productoSeleccionado by remember { mutableStateOf<Producto?>(null) }
    var varianteSeleccionada by remember { mutableStateOf<Variante?>(null) }
    var cantidad by remember { mutableStateOf("1") }

    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var comprobante by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }

    fun cargarDatos() {
        cargando = true
        db.collection("products")
            .get()
            .addOnSuccessListener { res ->
                productos = res.documents.map { productoDesdeDocumento(it) }
                cargando = false
            }
            .addOnFailureListener {
                cargando = false
                mensaje = "No se pudieron cargar los productos"
            }
    }

    LaunchedEffect(Unit) {
        cargarDatos()
    }

    val productosFiltrados = productos.filter { prod ->
        val texto = buscando.trim().lowercase()
        texto.isEmpty() ||
                prod.codigo.lowercase().contains(texto) ||
                prod.nombre.lowercase().contains(texto)
    }

    if (comprobante.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            MayLeVolverButton(onClick = onVolver)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Transferencia realizada",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = comprobante,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    comprobante = ""
                    destino = ""
                    productoSeleccionado = null
                    varianteSeleccionada = null
                    buscando = ""
                    cantidad = "1"
                    mensaje = ""
                    cargarDatos()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("NUEVA TRANSFERENCIA")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        MayLeVolverButton(onClick = onVolver)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "TRANSFERENCIAS DE STOCK",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (esAdmin) {
            OutlinedTextField(
                value = origen,
                onValueChange = { origen = it.take(10).uppercase() },
                label = { Text("Punto de venta Origen") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else {
            Text(
                text = "Origen: $origen",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = destino,
            onValueChange = { destino = it.take(10).uppercase() },
            label = { Text("Punto de venta Destino") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = buscando,
            onValueChange = {
                buscando = it
                productoSeleccionado = null
                varianteSeleccionada = null
            },
            label = { Text("Buscar producto por código o nombre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (cargando) {
            Text("Cargando productos...")
        } else if (productoSeleccionado == null) {
            if (productosFiltrados.isEmpty()) {
                Text("No se encontraron productos.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(productosFiltrados) { prod ->
                        OutlinedButton(
                            onClick = {
                                productoSeleccionado = prod
                                buscando = prod.nombre
                                varianteSeleccionada = null
                                mensaje = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = prod.nombre,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = "Código: " + prod.codigo)
                            }
                        }
                    }
                }
            }
        } else {
            val producto = productoSeleccionado!!
            val variantesDisponibles = producto.obtenerVariantesParaPuntoVenta(origen)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = producto.nombre,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Código: " + producto.codigo)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Seleccione variante a transferir",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (variantesDisponibles.isEmpty()) {
                Text("Este producto no tiene variantes registradas para el origen.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(150.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(variantesDisponibles) { variante ->
                        OutlinedButton(
                            onClick = {
                                varianteSeleccionada = variante
                                mensaje = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "${variante.nombre} | Stock en $origen: ${variante.stock}")
                        }
                    }
                }
            }

            if (varianteSeleccionada != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Variante seleccionada: ${varianteSeleccionada!!.nombre}",
                    fontWeight = FontWeight.Bold,
                    color = azulMayLe
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = cantidad,
                onValueChange = {
                    cantidad = it.filter { caracter -> caracter.isDigit() }
                },
                label = { Text("Cantidad a transferir") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val origenClean = origen.trim().uppercase()
                    val destinoClean = destino.trim().uppercase()
                    val cantLong = cantidad.toLongOrNull() ?: 0L

                    if (origenClean.isBlank() || destinoClean.isBlank()) {
                        mensaje = "Complete origen y destino"
                        return@Button
                    }

                    if (origenClean == destinoClean) {
                        mensaje = "El origen y destino deben ser diferentes"
                        return@Button
                    }

                    if (productoSeleccionado == null) {
                        mensaje = "Seleccione un producto"
                        return@Button
                    }

                    if (varianteSeleccionada == null) {
                        mensaje = "Seleccione una variante"
                        return@Button
                    }

                    if (cantLong <= 0) {
                        mensaje = "La cantidad debe ser mayor a cero"
                        return@Button
                    }

                    if (usuario == null) {
                        mensaje = "Sesión no válida"
                        return@Button
                    }

                    guardando = true
                    mensaje = ""

                    db.runTransaction { transaction ->
                        val referenciaProducto = db.collection("products").document(producto.id)
                        val documento = transaction.get(referenciaProducto)

                        val pvData = documento.get("puntosVenta") as? Map<*, *>
                        val mapaPuntosVenta = mutableMapOf<String, MutableMap<String, Any>>()

                        if (pvData != null) {
                            pvData.forEach { (k, v) ->
                                val pvName = k?.toString()?.trim()?.uppercase() ?: return@forEach
                                if (v is Map<*, *>) {
                                    val varMap = mutableMapOf<String, Any>()
                                    v.forEach { (vk, vv) ->
                                        if (vk != null) {
                                            val st = when (vv) {
                                                is Number -> vv.toLong()
                                                else -> vv?.toString()?.toLongOrNull() ?: 0L
                                            }
                                            varMap[vk.toString()] = st
                                        }
                                    }
                                    mapaPuntosVenta[pvName] = varMap
                                }
                            }
                        }

                        val mapaOrigen = mapaPuntosVenta.getOrPut(origenClean) {
                            val seed = mutableMapOf<String, Any>()
                            variantesDesdeDocumento(documento).forEach { seed[it.nombre] = it.stock }
                            seed
                        }

                        val mapaDestino = mapaPuntosVenta.getOrPut(destinoClean) {
                            val seed = mutableMapOf<String, Any>()
                            variantesDesdeDocumento(documento).forEach { seed[it.nombre] = it.stock }
                            seed
                        }

                        val vNombre = varianteSeleccionada!!.nombre
                        val stockOrigen = (mapaOrigen[vNombre] as? Number)?.toLong() ?: 0L
                        val stockDestino = (mapaDestino[vNombre] as? Number)?.toLong() ?: 0L

                        mapaOrigen[vNombre] = stockOrigen - cantLong
                        mapaDestino[vNombre] = stockDestino + cantLong

                        transaction.update(referenciaProducto, "puntosVenta", mapaPuntosVenta)

                        val refTransferencia = db.collection("transferencias").document()
                        val datosTransferencia = hashMapOf(
                            "usuarioId" to usuario.uid,
                            "usuarioEmail" to (usuario.email ?: ""),
                            "puntoVentaOrigen" to origenClean,
                            "puntoVentaDestino" to destinoClean,
                            "productoId" to producto.id,
                            "codigo" to producto.codigo,
                            "nombre" to producto.nombre,
                            "variante" to vNombre,
                            "cantidad" to cantLong,
                            "fechaHora" to FieldValue.serverTimestamp()
                        )

                        transaction.set(refTransferencia, datosTransferencia)
                        null
                    }
                        .addOnSuccessListener {
                            guardando = false
                            comprobante = buildString {
                                appendLine("MAYLE ORTOPEDIA")
                                appendLine("COMPROBANTE DE TRANSFERENCIA")
                                appendLine()
                                appendLine("Origen: $origenClean")
                                appendLine("Destino: $destinoClean")
                                appendLine("Producto: ${producto.nombre} (${producto.codigo})")
                                appendLine("Variante: ${varianteSeleccionada!!.nombre}")
                                appendLine("Cantidad enviada: $cantLong")
                                appendLine("Usuario: ${usuario.email ?: ""}")
                            }
                        }
                        .addOnFailureListener {
                            guardando = false
                            mensaje = "No se pudo realizar la transferencia"
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !guardando
            ) {
                Text(
                    text = if (guardando) "TRANSFERIENDO..." else "CONFIRMAR TRANSFERENCIA",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = mensaje,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MayLeProductosStock(
    esAdmin: Boolean,
    onVolver: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)

    var productos by remember { mutableStateOf(listOf<Producto>()) }
    var mostrandoFormulario by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(true) }
    var mensaje by remember { mutableStateOf("") }

    fun cargarProductos() {
        cargando = true
        FirebaseFirestore.getInstance()
            .collection("products")
            .get()
            .addOnSuccessListener { resultado ->
                productos = resultado.documents.map { productoDesdeDocumento(it) }
                cargando = false
            }
            .addOnFailureListener {
                mensaje = "No se pudieron cargar los productos"
                cargando = false
            }
    }

    LaunchedEffect(Unit) {
        cargarProductos()
    }

    if (mostrandoFormulario) {
        MayLeNuevoProducto(
            onVolver = { mostrandoFormulario = false },
            onProductoGuardado = {
                mostrandoFormulario = false
                cargarProductos()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        MayLeVolverButton(onClick = onVolver)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Productos y Stock",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (esAdmin) {
            Button(
                onClick = { mostrandoFormulario = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("NUEVO PRODUCTO")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (cargando) {
            Text("Cargando productos...")
        } else if (productos.isEmpty()) {
            Text("Todavía no hay productos cargados.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(productos) { producto ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = producto.nombre,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(text = "Código: " + producto.codigo)

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(text = "Efectivo: $" + formatoPrecio(producto.precioContado))
                            Text(text = "Un pago: $" + formatoPrecio(producto.precioUnPago))
                            Text(text = "3 cuotas: $" + formatoPrecio(producto.precioTresCuotas))

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Variantes y stock",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = azulMayLe
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            if (producto.stockPorPuntoVenta.isNotEmpty()) {
                                producto.stockPorPuntoVenta.forEach { (pv, listaVar) ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Punto de Venta $pv:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    listaVar.forEach { variante ->
                                        Text(text = "  ${variante.nombre}: ${variante.stock}")
                                    }
                                }
                            } else if (producto.variantesDefault.isNotEmpty()) {
                                producto.variantesDefault.forEach { variante ->
                                    Text(text = "${variante.nombre}: ${variante.stock}")
                                }
                            } else {
                                Text(text = "Sin variantes registradas")
                            }
                        }
                    }
                }
            }
        }

        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensaje,
                color = Color.Red
            )
        }
    }
}

@Composable
fun MayLeNuevoProducto(
    onVolver: () -> Unit,
    onProductoGuardado: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)

    var codigo by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var precioContado by remember { mutableStateOf("") }

    val variantes = remember {
        mutableStateListOf(VarianteEntrada())
    }

    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    val efectivo = precioContado.replace(",", ".").toDoubleOrNull()
    val precioUnPago = efectivo?.times(1.12) ?: 0.0
    val precioTresCuotas = efectivo?.times(1.30) ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        MayLeVolverButton(onClick = onVolver)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Nuevo producto",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it.take(30) },
            label = { Text("Código interno") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del producto") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = precioContado,
            onValueChange = { precioContado = it },
            label = { Text("Precio efectivo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = if (efectivo != null) formatoPrecio(precioUnPago) else "",
            onValueChange = {},
            label = { Text("Precio débito/crédito 1 pago") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = if (efectivo != null) formatoPrecio(precioTresCuotas) else "",
            onValueChange = {},
            label = { Text("Precio crédito 3 cuotas") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "1 pago × 1,12 | 3 cuotas × 1,30",
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "VARIANTES Y STOCK",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ejemplos: 1, 2, 3, 4, 5, 6, U, CH, M, G, XG, Piel, Negro, Tostado, Blanco..."
        )

        Spacer(modifier = Modifier.height(12.dp))

        variantes.forEachIndexed { indice, variante ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = variante.nombre,
                    onValueChange = { variantes[indice] = variante.copy(nombre = it.take(20)) },
                    label = { Text("Variante") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = variante.stock,
                    onValueChange = {
                        variantes[indice] = variante.copy(
                            stock = it.filter { caracter -> caracter.isDigit() }
                        )
                    },
                    label = { Text("Stock") },
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.width(4.dp))

                TextButton(
                    onClick = {
                        if (variantes.size > 1) {
                            variantes.removeAt(indice)
                        } else {
                            variantes[0] = VarianteEntrada()
                        }
                    }
                ) {
                    Text("ELIMINAR", color = Color.Red, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = {
                variantes.add(VarianteEntrada())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("AGREGAR VARIANTE")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (codigo.isBlank() || nombre.isBlank() || efectivo == null) {
                    mensaje = "Complete código, nombre y precio efectivo"
                    return@Button
                }

                val variantesValidas = variantes
                    .map {
                        VarianteEntrada(
                            nombre = it.nombre.trim(),
                            stock = it.stock
                        )
                    }
                    .filter { it.nombre.isNotEmpty() }

                if (variantesValidas.isEmpty()) {
                    mensaje = "Agregue al menos una variante"
                    return@Button
                }

                val nombres = variantesValidas.map { it.nombre.lowercase() }
                if (nombres.size != nombres.toSet().size) {
                    mensaje = "No puede haber variantes repetidas"
                    return@Button
                }

                guardando = true
                mensaje = ""

                val db = FirebaseFirestore.getInstance()

                db.collection("products")
                    .whereEqualTo("codigo", codigo.trim())
                    .get()
                    .addOnSuccessListener { resultado ->
                        if (!resultado.isEmpty) {
                            guardando = false
                            mensaje = "Ya existe un producto con ese código"
                            return@addOnSuccessListener
                        }

                        val mapaVariantes = variantesValidas.associate {
                            it.nombre to (it.stock.toLongOrNull() ?: 0L)
                        }

                        val producto = hashMapOf(
                            "codigo" to codigo.trim(),
                            "nombre" to nombre.trim(),
                            "precioContado" to efectivo,
                            "precioUnPago" to precioUnPago,
                            "precioTresCuotas" to precioTresCuotas,
                            "variantes" to mapaVariantes
                        )

                        db.collection("products")
                            .add(producto)
                            .addOnSuccessListener {
                                guardando = false
                                onProductoGuardado()
                            }
                            .addOnFailureListener {
                                guardando = false
                                mensaje = "No se pudo guardar el producto"
                            }
                    }
                    .addOnFailureListener {
                        guardando = false
                        mensaje = "No se pudo comprobar el código"
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !guardando
        ) {
            Text(
                text = if (guardando) "GUARDANDO..." else "GUARDAR PRODUCTO",
                fontWeight = FontWeight.Bold
            )
        }

        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensaje,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MayLeVolverButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(54.dp)
    ) {
        Text(
            text = "← VOLVER",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MayLeModuloButton(
    texto: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(
            text = texto,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
