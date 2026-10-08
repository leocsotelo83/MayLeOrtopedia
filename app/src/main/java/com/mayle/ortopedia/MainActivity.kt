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
import androidx.compose.material3.AlertDialog
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
import com.google.firebase.firestore.SetOptions
import com.mayle.ortopedia.ui.theme.MayLeOrtopediaTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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

data class VentaHistorialItem(
    val productoId: String = "",
    val codigo: String = "",
    val nombre: String = "",
    val variante: String = "",
    val cantidad: Long = 0L,
    val precioUnitario: Double = 0.0,
    val subtotal: Double = 0.0,
    val formaPago: String = ""
)

data class VentaHistorial(
    val id: String = "",
    val usuarioId: String = "",
    val usuarioEmail: String = "",
    val puntoVenta: String = "",
    val nombreComprador: String = "",
    val formaPago: String = "",
    val total: Double = 0.0,
    val comision: Double = 0.0,
    val estado: String = "confirmada",
    val fechaTexto: String = "",
    val fechaMillis: Long = 0L,
    val items: List<VentaHistorialItem> = emptyList(),
    val motivoAnulacion: String = "",
    val anuladaPorEmail: String = ""
)

data class UsuarioApp(
    val id: String = "",
    val email: String = "",
    val role: String = "vendedor",
    val puntoVenta: String = "",
    val activo: Boolean = true,
    val montoAperturaCaja: Double = 0.0
)

data class MovimientoCaja(
    val id: String = "",
    val usuarioId: String = "",
    val usuarioEmail: String = "",
    val puntoVenta: String = "",
    val tipo: String = "",
    val importe: Double = 0.0,
    val concepto: String = "",
    val formaPago: String = "",
    val ventaId: String = "",
    val fechaMillis: Long = 0L
)

data class RendicionCaja(
    val id: String = "",
    val usuarioId: String = "",
    val usuarioEmail: String = "",
    val puntoVenta: String = "",
    val saldoEsperado: Double = 0.0,
    val montoDeclarado: Double = 0.0,
    val diferencia: Double = 0.0,
    val nuevaApertura: Double = 0.0,
    val estado: String = "pendiente",
    val fechaMillis: Long = 0L,
    val confirmadoPorEmail: String = ""
)

data class PagoComision(
    val id: String = "",
    val vendedorId: String = "",
    val vendedorEmail: String = "",
    val monto: Double = 0.0,
    val estado: String = "PENDIENTE_ACEPTACION",
    val ordenadoPorId: String = "",
    val ordenadoPorEmail: String = "",
    val fechaMillis: Long = 0L,
    val aceptadoPorId: String = "",
    val aceptadoPorEmail: String = "",
    val fechaAceptacionMillis: Long = 0L
)

data class ComisionResumen(
    val usuarioId: String = "",
    val email: String = "",
    val puntoVenta: String = "",
    val generada: Double = 0.0,
    val pagada: Double = 0.0,
    val pendiente: Double = 0.0
)

data class ReporteStockFila(
    val productoId: String = "",
    val codigo: String = "",
    val nombre: String = "",
    val variante: String = "",
    val puntoVenta: String = "",
    val stock: Long = 0L,
    val precio: Double = 0.0
)

data class ReporteStockRapidoFila(
    val productoId: String = "",
    val codigo: String = "",
    val nombre: String = "",
    val variante: String = "",
    val stockTotal: Long = 0L
)

data class ReporteProductoVarianteFila(
    val producto: Producto,
    val variante: String = "",
    val unidadesVendidas: Long = 0L,
    val importeVendido: Double = 0.0,
    val stockTotal: Long = 0L
)

data class VarianteEntrada(
    var nombre: String = "",
    var stock: String = "0"
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
        activo = documento.getBoolean("activo") ?: true,
        montoAperturaCaja = documento.getDouble("montoAperturaCaja") ?: 0.0
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

        "historial" -> {
            MayLeHistorialVentas(
                usuarioId = usuario?.uid ?: "",
                esAdmin = rol == "admin",
                onVolver = { pantalla = "menu" }
            )
        }

        "comisiones" -> {
            MayLeComisiones(
                usuarioId = usuario?.uid ?: "",
                esAdmin = rol == "admin",
                onVolver = { pantalla = "menu" }
            )
        }

        "caja" -> {
            MayLeCaja(
                usuarioId = usuario?.uid ?: "",
                usuarioEmail = usuario?.email ?: "",
                puntoVenta = if (rol == "admin" || puntoVenta.isBlank()) "SIN ASIGNAR" else puntoVenta,
                esAdmin = rol == "admin",
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

        "reportes" -> {
            MayLeReportes(
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

                MayLeModuloButton(texto = "HISTORIAL DE VENTAS") {
                    pantalla = "historial"
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

                MayLeModuloButton(texto = "CAJA") {
                    pantalla = "caja"
                }

                Spacer(modifier = Modifier.height(14.dp))

                MayLeModuloButton(texto = "COMISIONES") {
                    pantalla = "comisiones"
                }

                if (rol == "admin") {
                    Spacer(modifier = Modifier.height(14.dp))

                    MayLeModuloButton(texto = "ADMINISTRACIÓN") {
                        pantalla = "administracion"
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    MayLeModuloButton(texto = "REPORTES") {
                        pantalla = "reportes"
                    }
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

    when (pantalla) {
        "usuarios" -> {
            MayLeUsuarios(onVolver = { pantalla = "menu" })
            return
        }

        "puntosVenta" -> {
            MayLePuntosVenta(onVolver = { pantalla = "menu" })
            return
        }

        "comisiones" -> {
            MayLeConfiguracionComision(onVolver = { pantalla = "menu" })
            return
        }
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

        Spacer(modifier = Modifier.height(14.dp))

        MayLeModuloButton(texto = "CONFIGURAR PUNTOS DE VENTA") {
            pantalla = "puntosVenta"
        }

        Spacer(modifier = Modifier.height(14.dp))

        MayLeModuloButton(texto = "CONFIGURACIÓN DE COMISIONES") {
            pantalla = "comisiones"
        }
    }
}

@Composable
fun MayLePuntosVenta(
    onVolver: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)
    val db = FirebaseFirestore.getInstance()

    var punto1 by remember { mutableStateOf("SANTIAGO") }
    var punto2 by remember { mutableStateOf("") }
    var punto3 by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("configuracion")
            .document("puntosVenta")
            .get()
            .addOnSuccessListener { documento ->
                punto1 = documento.getString("puntoVenta1") ?: "SANTIAGO"
                punto2 = documento.getString("puntoVenta2") ?: ""
                punto3 = documento.getString("puntoVenta3") ?: ""
                cargando = false
            }
            .addOnFailureListener {
                cargando = false
                mensaje = "No se pudo cargar la configuración de puntos de venta"
            }
    }

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
            text = "Puntos de venta",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Podés configurar hasta 3 puntos de venta. Cada nombre puede tener hasta 10 caracteres.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = punto1,
            onValueChange = { punto1 = it.take(10).uppercase() },
            label = { Text("Punto de venta 1") },
            supportingText = { Text("${punto1.length}/10") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !cargando && !guardando
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = punto2,
            onValueChange = { punto2 = it.take(10).uppercase() },
            label = { Text("Punto de venta 2") },
            supportingText = { Text("${punto2.length}/10") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !cargando && !guardando
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = punto3,
            onValueChange = { punto3 = it.take(10).uppercase() },
            label = { Text("Punto de venta 3") },
            supportingText = { Text("${punto3.length}/10") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !cargando && !guardando
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Los vendedores podrán ser asignados únicamente a estos puntos de venta.",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val valores = listOf(punto1, punto2, punto3)
                    .map { it.trim().uppercase() }
                    .filter { it.isNotBlank() }

                if (valores.isEmpty()) {
                    mensaje = "Configure al menos un punto de venta"
                    return@Button
                }

                if (valores.any { it.length > 10 }) {
                    mensaje = "Cada punto de venta admite hasta 10 caracteres"
                    return@Button
                }

                if (valores.distinct().size != valores.size) {
                    mensaje = "Los puntos de venta no pueden repetirse"
                    return@Button
                }

                guardando = true
                mensaje = ""

                db.collection("configuracion")
                    .document("puntosVenta")
                    .set(
                        mapOf(
                            "puntoVenta1" to punto1.trim().uppercase(),
                            "puntoVenta2" to punto2.trim().uppercase(),
                            "puntoVenta3" to punto3.trim().uppercase()
                        ),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        punto1 = punto1.trim().uppercase()
                        punto2 = punto2.trim().uppercase()
                        punto3 = punto3.trim().uppercase()
                        guardando = false
                        mensaje = "Puntos de venta guardados correctamente"
                    }
                    .addOnFailureListener { error ->
                        guardando = false
                        mensaje = "No se pudieron guardar los puntos de venta: ${error.message ?: "error"}"
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !cargando && !guardando
        ) {
            Text(
                text = if (guardando) "GUARDANDO..." else "GUARDAR PUNTOS DE VENTA",
                fontWeight = FontWeight.Bold
            )
        }

        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensaje,
                color = if (mensaje.startsWith("Puntos")) Color(0xFF2E7D32) else Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MayLeConfiguracionComision(
    onVolver: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)
    val db = FirebaseFirestore.getInstance()

    var porcentaje by remember { mutableStateOf("10") }
    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("configuracion")
            .document("general")
            .get()
            .addOnSuccessListener { documento ->
                val valor = documento.get("porcentajeComision")
                porcentaje = when (valor) {
                    is Number -> formatoPrecio(valor.toDouble())
                    else -> valor?.toString() ?: "10"
                }
                if (porcentaje.isBlank()) {
                    porcentaje = "10"
                }
                cargando = false
            }
            .addOnFailureListener {
                cargando = false
                mensaje = "No se pudo cargar la configuración. Se mantiene 10% como valor predeterminado."
            }
    }

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
            text = "Configuración de comisiones",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Porcentaje de comisión para nuevas ventas",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = porcentaje,
            onValueChange = { porcentaje = it.filter { caracter -> caracter.isDigit() || caracter == ',' || caracter == '.' } },
            label = { Text("Comisión (%)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            enabled = !cargando && !guardando
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Valor actual: ${porcentaje.replace(',', '.')}%",
            color = azulMayLe,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "El cambio se aplicará solamente a ventas nuevas. Las comisiones de ventas ya registradas no se modifican.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val nuevoPorcentaje = porcentaje.replace(',', '.').toDoubleOrNull()

                if (nuevoPorcentaje == null) {
                    mensaje = "Ingrese un porcentaje válido"
                    return@Button
                }

                if (nuevoPorcentaje < 0.0 || nuevoPorcentaje > 100.0) {
                    mensaje = "El porcentaje debe estar entre 0 y 100"
                    return@Button
                }

                guardando = true
                mensaje = ""

                db.collection("configuracion")
                    .document("general")
                    .set(
                        mapOf(
                            "porcentajeComision" to nuevoPorcentaje
                        ),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        guardando = false
                        porcentaje = formatoPrecio(nuevoPorcentaje)
                        mensaje = "Configuración guardada correctamente"
                    }
                    .addOnFailureListener { error ->
                        guardando = false
                        mensaje = "No se pudo guardar la configuración: ${error.message ?: "error"}"
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !cargando && !guardando
        ) {
            Text(
                text = if (guardando) "GUARDANDO..." else "GUARDAR COMISIÓN",
                fontWeight = FontWeight.Bold
            )
        }

        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensaje,
                color = if (mensaje.startsWith("Configuración")) Color(0xFF2E7D32) else Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MayLeSeleccionarPuntoVenta(
    puntoVentaActual: String,
    opciones: List<String>,
    onSeleccion: (String) -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { mostrarDialogo = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (puntoVentaActual.isBlank()) {
                "SELECCIONAR PUNTO DE VENTA"
            } else {
                "Punto de venta: $puntoVentaActual"
            },
            fontWeight = FontWeight.Bold
        )
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Seleccionar punto de venta") },
            text = {
                Column {
                    if (opciones.isEmpty()) {
                        Text("No hay puntos de venta configurados.")
                    } else {
                        opciones.forEach { opcion ->
                            if (opcion.equals(puntoVentaActual, ignoreCase = true)) {
                                Button(
                                    onClick = {
                                        onSeleccion(opcion)
                                        mostrarDialogo = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(opcion, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        onSeleccion(opcion)
                                        mostrarDialogo = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(opcion)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarDialogo = false }) {
                    Text("CANCELAR")
                }
            }
        )
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
    val db = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var puntoVenta by remember { mutableStateOf("") }
    var montoAperturaCaja by remember { mutableStateOf("0") }
    var rol by remember { mutableStateOf("vendedor") }
    var opcionesPuntosVenta by remember { mutableStateOf(listOf<String>()) }
    var guardando by remember { mutableStateOf(false) }
    var cargandoPuntosVenta by remember { mutableStateOf(true) }
    var mensaje by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("configuracion")
            .document("puntosVenta")
            .get()
            .addOnSuccessListener { documento ->
                opcionesPuntosVenta = listOf(
                    documento.getString("puntoVenta1") ?: "",
                    documento.getString("puntoVenta2") ?: "",
                    documento.getString("puntoVenta3") ?: ""
                ).map { it.trim().uppercase() }.filter { it.isNotBlank() }
                cargandoPuntosVenta = false
            }
            .addOnFailureListener {
                cargandoPuntosVenta = false
                mensaje = "No se pudieron cargar los puntos de venta"
            }
    }

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
                onClick = {
                    rol = "vendedor"
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("VENDEDOR")
            }

            Button(
                onClick = {
                    rol = "admin"
                    puntoVenta = ""
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("ADMIN")
            }
        }

        if (rol == "vendedor") {
            Spacer(modifier = Modifier.height(14.dp))

            MayLeSeleccionarPuntoVenta(
                puntoVentaActual = puntoVenta,
                opciones = opcionesPuntosVenta,
                onSeleccion = { puntoVenta = it }
            )

            if (!cargandoPuntosVenta && opcionesPuntosVenta.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Configure primero los puntos de venta en Administración.",
                    color = Color.Red,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = montoAperturaCaja,
            onValueChange = { montoAperturaCaja = it.filter { caracter -> caracter.isDigit() } },
            label = { Text("Apertura inicial de caja") },
            supportingText = { Text("Monto de cambio inicial") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

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
                    mensaje = "Seleccione el punto de venta para el vendedor"
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
                    montoAperturaCaja = montoAperturaCaja.toDoubleOrNull() ?: 0.0,
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
            enabled = !guardando && !cargandoPuntosVenta
        ) {
            Text(
                text = if (guardando) "CREANDO..." else "CREAR USUARIO",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "El usuario quedará activo con el punto de venta seleccionado.",
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
    montoAperturaCaja: Double,
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
                "activo" to true,
                "montoAperturaCaja" to montoAperturaCaja
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
    val db = FirebaseFirestore.getInstance()

    var rol by remember { mutableStateOf(usuario.role) }
    var puntoVenta by remember { mutableStateOf(usuario.puntoVenta) }
    var montoAperturaCaja by remember { mutableStateOf(formatoPrecio(usuario.montoAperturaCaja)) }
    var activo by remember { mutableStateOf(usuario.activo) }
    var opcionesPuntosVenta by remember { mutableStateOf(listOf<String>()) }
    var cargandoPuntosVenta by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    LaunchedEffect(usuario.id) {
        db.collection("configuracion")
            .document("puntosVenta")
            .get()
            .addOnSuccessListener { documento ->
                opcionesPuntosVenta = listOf(
                    documento.getString("puntoVenta1") ?: "",
                    documento.getString("puntoVenta2") ?: "",
                    documento.getString("puntoVenta3") ?: ""
                ).map { it.trim().uppercase() }.filter { it.isNotBlank() }.toMutableList().apply {
                    if (usuario.puntoVenta.isNotBlank() && !contains(usuario.puntoVenta.trim().uppercase())) {
                        add(usuario.puntoVenta.trim().uppercase())
                    }
                }
                cargandoPuntosVenta = false
            }
            .addOnFailureListener {
                cargandoPuntosVenta = false
                opcionesPuntosVenta = if (usuario.puntoVenta.isNotBlank()) listOf(usuario.puntoVenta.trim().uppercase()) else emptyList()
                mensaje = "No se pudieron cargar los puntos de venta configurados"
            }
    }

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
                onClick = {
                    rol = "admin"
                    puntoVenta = ""
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("ADMIN")
            }
        }

        if (rol == "vendedor") {
            Spacer(modifier = Modifier.height(14.dp))

            MayLeSeleccionarPuntoVenta(
                puntoVentaActual = puntoVenta,
                opciones = opcionesPuntosVenta,
                onSeleccion = { puntoVenta = it }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = montoAperturaCaja,
            onValueChange = { montoAperturaCaja = it.filter { caracter -> caracter.isDigit() } },
            label = { Text("Apertura inicial de caja") },
            supportingText = { Text("Se usa al iniciar el primer ciclo de caja") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

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
                    mensaje = "Seleccione el punto de venta para el vendedor"
                    return@Button
                }

                guardando = true
                mensaje = ""

                val pvLimpio = if (rol == "admin") "" else puntoVenta.trim().take(10).uppercase()

                val cambios = hashMapOf<String, Any>(
                    "role" to rol,
                    "puntoVenta" to pvLimpio,
                    "activo" to activo,
                    "montoAperturaCaja" to (montoAperturaCaja.toDoubleOrNull() ?: 0.0)
                )

                db.collection("users")
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
            enabled = !guardando && !cargandoPuntosVenta
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
    var porcentajeComision by remember { mutableStateOf(10.0) }
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

        FirebaseFirestore.getInstance()
            .collection("configuracion")
            .document("general")
            .get()
            .addOnSuccessListener { documento ->
                porcentajeComision = when (val valor = documento.get("porcentajeComision")) {
                    is Number -> valor.toDouble()
                    else -> valor?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: 10.0
                }.coerceIn(0.0, 100.0)
            }
            .addOnFailureListener {
                porcentajeComision = 10.0
            }
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Comisión de la venta: ${formatoPrecio(porcentajeComision)}%",
                color = Color.Gray,
                fontSize = 14.sp
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

                        val comisionCalculada = totalCarrito * (porcentajeComision / 100.0)

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

                        if (formaPago == "EFECTIVO") {
                            val refMovimientoCaja = db.collection("movimientosCaja").document()
                            val datosMovimientoCaja = hashMapOf(
                                "tipo" to "VENTA_EFECTIVO",
                                "importe" to totalCarrito,
                                "usuarioId" to usuario.uid,
                                "usuarioEmail" to (usuario.email ?: ""),
                                "puntoVenta" to puntoVenta.take(10).uppercase(),
                                "formaPago" to formaPago,
                                "ventaId" to referenciaVenta.id,
                                "concepto" to "Venta en efectivo",
                                "fechaHora" to FieldValue.serverTimestamp()
                            )
                            transaction.set(refMovimientoCaja, datosMovimientoCaja)
                        }

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
                                appendLine("Comisión generada: $" + formatoPrecio(totalCarrito * (porcentajeComision / 100.0)))
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
fun MayLeHistorialVentas(
    usuarioId: String,
    esAdmin: Boolean,
    onVolver: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)
    val db = FirebaseFirestore.getInstance()
    val usuarioActual = FirebaseAuth.getInstance().currentUser

    var ventas by remember { mutableStateOf(listOf<VentaHistorial>()) }
    var cargando by remember { mutableStateOf(true) }
    var mensaje by remember { mutableStateOf("") }
    var ventaSeleccionada by remember { mutableStateOf<VentaHistorial?>(null) }
    var motivoAnulacion by remember { mutableStateOf("") }
    var anulando by remember { mutableStateOf(false) }

    fun cargarVentas() {
        cargando = true
        mensaje = ""

        db.collection("ventas")
            .get()
            .addOnSuccessListener { resultado ->
                ventas = resultado.documents
                    .map { documento ->
                        val itemsRaw = documento.get("items") as? List<*> ?: emptyList<Any>()
                        val items = itemsRaw.mapNotNull { elemento ->
                            val mapa = elemento as? Map<*, *> ?: return@mapNotNull null
                            VentaHistorialItem(
                                productoId = mapa["productoId"]?.toString() ?: "",
                                codigo = mapa["codigo"]?.toString() ?: "",
                                nombre = mapa["nombre"]?.toString() ?: "",
                                variante = mapa["variante"]?.toString() ?: "",
                                cantidad = (mapa["cantidad"] as? Number)?.toLong()
                                    ?: mapa["cantidad"]?.toString()?.toLongOrNull()
                                    ?: 0L,
                                precioUnitario = (mapa["precioUnitario"] as? Number)?.toDouble()
                                    ?: mapa["precioUnitario"]?.toString()?.toDoubleOrNull()
                                    ?: 0.0,
                                subtotal = (mapa["subtotal"] as? Number)?.toDouble()
                                    ?: mapa["subtotal"]?.toString()?.toDoubleOrNull()
                                    ?: 0.0,
                                formaPago = mapa["formaPago"]?.toString() ?: ""
                            )
                        }

                        val timestamp = documento.getTimestamp("fechaHora")
                        val fechaMillis = timestamp?.toDate()?.time ?: 0L
                        val fechaTexto = timestamp?.toDate()?.let {
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                        } ?: "Fecha pendiente"

                        VentaHistorial(
                            id = documento.id,
                            usuarioId = documento.getString("usuarioId") ?: "",
                            usuarioEmail = documento.getString("usuarioEmail") ?: "",
                            puntoVenta = documento.getString("puntoVenta") ?: "",
                            nombreComprador = documento.getString("nombreComprador") ?: "",
                            formaPago = documento.getString("formaPago") ?: "",
                            total = documento.getDouble("total") ?: 0.0,
                            comision = documento.getDouble("comision") ?: 0.0,
                            estado = documento.getString("estado") ?: "confirmada",
                            fechaTexto = fechaTexto,
                            fechaMillis = fechaMillis,
                            items = items,
                            motivoAnulacion = documento.getString("motivoAnulacion") ?: "",
                            anuladaPorEmail = documento.getString("anuladaPorEmail") ?: ""
                        )
                    }
                    .filter { esAdmin || it.usuarioId == usuarioId }
                    .sortedByDescending { it.fechaMillis }

                cargando = false
            }
            .addOnFailureListener {
                cargando = false
                mensaje = "No se pudieron cargar las ventas"
            }
    }

    LaunchedEffect(usuarioId, esAdmin) {
        cargarVentas()
    }

    if (ventaSeleccionada != null) {
        val venta = ventaSeleccionada!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            MayLeVolverButton(
                onClick = {
                    ventaSeleccionada = null
                    motivoAnulacion = ""
                    mensaje = ""
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "DETALLE DE VENTA",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Venta: ${venta.id.takeLast(6)}", fontWeight = FontWeight.Bold)
            Text("Fecha: ${venta.fechaTexto}")
            Text("Vendedor: ${venta.usuarioEmail}")
            Text("Punto de venta: ${venta.puntoVenta}")
            Text("Comprador: ${venta.nombreComprador}")
            Text("Forma de pago: ${venta.formaPago}")
            Text("Estado: ${venta.estado.uppercase()}")

            Spacer(modifier = Modifier.height(16.dp))

            venta.items.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.nombre, fontWeight = FontWeight.Bold)
                        Text("Código: ${item.codigo}")
                        Text("Variante: ${item.variante}")
                        Text("Cantidad: ${item.cantidad}")
                        Text("Subtotal: $${formatoPrecio(item.subtotal)}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "TOTAL: $${formatoPrecio(venta.total)}",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )

            if (venta.estado.lowercase() == "anulada") {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Motivo de anulación: ${venta.motivoAnulacion}")
                Text("Anulada por: ${venta.anuladaPorEmail}")
            } else {
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = motivoAnulacion,
                    onValueChange = { motivoAnulacion = it },
                    label = { Text("Motivo de anulación (obligatorio)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (motivoAnulacion.isBlank()) {
                            mensaje = "Debe indicar el motivo de la anulación"
                            return@Button
                        }

                        if (usuarioActual == null) {
                            mensaje = "La sesión no es válida"
                            return@Button
                        }

                        anulando = true
                        mensaje = ""

                        db.runTransaction { transaction ->
                            val referenciaVenta = db.collection("ventas").document(venta.id)
                            val ventaActual = transaction.get(referenciaVenta)
                            val estadoActual = ventaActual.getString("estado") ?: "confirmada"

                            if (estadoActual.lowercase() == "anulada") {
                                throw IllegalStateException("Esta venta ya fue anulada")
                            }

                            val vendedorVenta = ventaActual.getString("usuarioId") ?: ""
                            if (!esAdmin && vendedorVenta != usuarioId) {
                                throw IllegalStateException("No puede anular una venta de otro vendedor")
                            }

                            val itemsActuales = ventaActual.get("items") as? List<*> ?: emptyList<Any>()
                            val puntoVentaVenta = ventaActual.getString("puntoVenta")?.trim()?.uppercase()
                                ?: "SIN ASIGNAR"

                            val cantidadesPorProducto = mutableMapOf<String, MutableMap<String, Long>>()

                            itemsActuales.forEach { elemento ->
                                val mapa = elemento as? Map<*, *> ?: return@forEach
                                val productoId = mapa["productoId"]?.toString() ?: return@forEach
                                val variante = mapa["variante"]?.toString() ?: return@forEach
                                val cantidad = (mapa["cantidad"] as? Number)?.toLong()
                                    ?: mapa["cantidad"]?.toString()?.toLongOrNull()
                                    ?: 0L

                                if (productoId.isNotBlank() && variante.isNotBlank() && cantidad > 0) {
                                    val variantesProducto = cantidadesPorProducto.getOrPut(productoId) {
                                        mutableMapOf()
                                    }
                                    variantesProducto[variante] =
                                        (variantesProducto[variante] ?: 0L) + cantidad
                                }
                            }

                            val actualizaciones = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, MutableMap<String, Any>>>>()

                            cantidadesPorProducto.forEach { (productoId, cantidadesVariantes) ->
                                val referenciaProducto = db.collection("products").document(productoId)
                                val documentoProducto = transaction.get(referenciaProducto)

                                val pvData = documentoProducto.get("puntosVenta") as? Map<*, *>
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

                                val mapaVariantes = mapaPuntosVenta.getOrPut(puntoVentaVenta) {
                                    val seed = mutableMapOf<String, Any>()
                                    variantesDesdeDocumento(documentoProducto).forEach {
                                        seed[it.nombre] = it.stock
                                    }
                                    seed
                                }

                                cantidadesVariantes.forEach { (variante, cantidad) ->
                                    val stockActual = (mapaVariantes[variante] as? Number)?.toLong() ?: 0L
                                    mapaVariantes[variante] = stockActual + cantidad
                                }

                                actualizaciones.add(referenciaProducto to mapaPuntosVenta)
                            }

                            actualizaciones.forEach { (referenciaProducto, mapaPuntosVenta) ->
                                transaction.update(referenciaProducto, "puntosVenta", mapaPuntosVenta)
                            }

                            transaction.update(
                                referenciaVenta,
                                mapOf(
                                    "estado" to "anulada",
                                    "motivoAnulacion" to motivoAnulacion.trim(),
                                    "anuladaPorId" to usuarioActual.uid,
                                    "anuladaPorEmail" to (usuarioActual.email ?: ""),
                                    "fechaAnulacion" to FieldValue.serverTimestamp()
                                )
                            )

                            val formaPagoVenta = ventaActual.getString("formaPago") ?: ""
                            if (formaPagoVenta == "EFECTIVO") {
                                val totalVenta = ventaActual.getDouble("total") ?: 0.0
                                val refMovimientoCaja = db.collection("movimientosCaja").document()
                                val datosMovimientoCaja = hashMapOf(
                                    "tipo" to "ANULACION_VENTA_EFECTIVO",
                                    "importe" to -totalVenta,
                                    "usuarioId" to usuarioActual.uid,
                                    "usuarioEmail" to (usuarioActual.email ?: ""),
                                    "puntoVenta" to puntoVentaVenta,
                                    "formaPago" to formaPagoVenta,
                                    "ventaId" to venta.id,
                                    "concepto" to "Anulación de venta en efectivo",
                                    "fechaHora" to FieldValue.serverTimestamp()
                                )
                                transaction.set(refMovimientoCaja, datosMovimientoCaja)
                            }
                            null
                        }
                        .addOnSuccessListener {
                                anulando = false
                                mensaje = "Venta anulada y stock reintegrado"
                                ventaSeleccionada = null
                                motivoAnulacion = ""
                                cargarVentas()
                            }
                            .addOnFailureListener { error ->
                                anulando = false
                                mensaje = error.message ?: "No se pudo anular la venta"
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !anulando
                ) {
                    Text(
                        if (anulando) "ANULANDO..." else "ANULAR VENTA",
                        fontWeight = FontWeight.Bold
                    )
                }
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
            text = "HISTORIAL DE VENTAS",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (esAdmin) "Mostrando todas las ventas" else "Mostrando tus ventas"
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (cargando) {
            Text("Cargando ventas...")
        } else if (ventas.isEmpty()) {
            Text("No hay ventas registradas.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ventas) { venta ->
                    OutlinedButton(
                        onClick = {
                            ventaSeleccionada = venta
                            motivoAnulacion = ""
                            mensaje = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Fecha: ${venta.fechaTexto}",
                                fontWeight = FontWeight.Bold
                            )
                            Text("Vendedor: ${venta.usuarioEmail}")
                            Text("Punto de venta: ${venta.puntoVenta}")
                            Text(
                                text = "Total: $${formatoPrecio(venta.total)}",
                                fontWeight = FontWeight.Bold
                            )
                            Text("Estado: ${venta.estado.uppercase()}")
                        }
                    }
                }
            }
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
fun MayLeComisiones(
    usuarioId: String,
    esAdmin: Boolean,
    onVolver: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val usuarioActualEmail = auth.currentUser?.email ?: ""

    var ventas by remember { mutableStateOf(listOf<VentaHistorial>()) }
    var pagos by remember { mutableStateOf(listOf<PagoComision>()) }
    var usuariosVendedores by remember { mutableStateOf(listOf<UsuarioApp>()) }
    var cargando by remember { mutableStateOf(true) }
    var mensaje by remember { mutableStateOf("") }
    var vendedorSeleccionado by remember { mutableStateOf<UsuarioApp?>(null) }
    var montoOrden by remember { mutableStateOf("") }
    var procesando by remember { mutableStateOf(false) }

    fun cargarComisiones() {
        cargando = true
        mensaje = ""

        db.collection("ventas")
            .get()
            .addOnSuccessListener { ventasSnap ->
                val ventasCargadas = ventasSnap.documents.map { documento ->
                    val timestamp = documento.getTimestamp("fechaHora")
                    val fechaMillis = timestamp?.toDate()?.time ?: 0L
                    val fechaTexto = timestamp?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                    } ?: "Fecha pendiente"

                    VentaHistorial(
                        id = documento.id,
                        usuarioId = documento.getString("usuarioId") ?: "",
                        usuarioEmail = documento.getString("usuarioEmail") ?: "",
                        puntoVenta = documento.getString("puntoVenta") ?: "",
                        nombreComprador = documento.getString("nombreComprador") ?: "",
                        formaPago = documento.getString("formaPago") ?: "",
                        total = documento.getDouble("total") ?: 0.0,
                        comision = documento.getDouble("comision") ?: 0.0,
                        estado = documento.getString("estado") ?: "confirmada",
                        fechaTexto = fechaTexto,
                        fechaMillis = fechaMillis
                    )
                }

                val consultaPagos = if (esAdmin) {
                    db.collection("pagosComisiones")
                        .get()
                } else {
                    db.collection("pagosComisiones")
                        .whereEqualTo("vendedorId", usuarioId)
                        .get()
                }

                consultaPagos
                    .addOnSuccessListener { pagosSnap ->
                        val pagosCargados = pagosSnap.documents.map { documento ->
                            val fecha = documento.getTimestamp("fechaOrden")?.toDate()?.time ?: 0L
                            val fechaAceptacion = documento.getTimestamp("fechaAceptacion")?.toDate()?.time ?: 0L

                            PagoComision(
                                id = documento.id,
                                vendedorId = documento.getString("vendedorId") ?: "",
                                vendedorEmail = documento.getString("vendedorEmail") ?: "",
                                monto = documento.getDouble("monto") ?: 0.0,
                                estado = documento.getString("estado") ?: "PENDIENTE_ACEPTACION",
                                ordenadoPorId = documento.getString("ordenadoPorId") ?: "",
                                ordenadoPorEmail = documento.getString("ordenadoPorEmail") ?: "",
                                fechaMillis = fecha,
                                aceptadoPorId = documento.getString("aceptadoPorId") ?: "",
                                aceptadoPorEmail = documento.getString("aceptadoPorEmail") ?: "",
                                fechaAceptacionMillis = fechaAceptacion
                            )
                        }

                        fun leerUsuario(documento: DocumentSnapshot): UsuarioApp {
                            return UsuarioApp(
                                id = documento.id,
                                email = documento.getString("email") ?: "",
                                role = documento.getString("role") ?: "vendedor",
                                puntoVenta = documento.getString("puntoVenta") ?: "",
                                activo = documento.getBoolean("activo") ?: true,
                                montoAperturaCaja = documento.getDouble("montoAperturaCaja") ?: 0.0
                            )
                        }

                        if (esAdmin) {
                            db.collection("users")
                                .whereEqualTo("role", "vendedor")
                                .get()
                                .addOnSuccessListener { usuariosSnap ->
                                    ventas = ventasCargadas.sortedByDescending { it.fechaMillis }
                                    pagos = pagosCargados.sortedByDescending { it.fechaMillis }
                                    usuariosVendedores = usuariosSnap.documents
                                        .map { leerUsuario(it) }
                                        .filter { it.activo }
                                        .sortedBy { it.email.lowercase() }
                                    cargando = false
                                }
                                .addOnFailureListener { error ->
                                    cargando = false
                                    mensaje = "No se pudieron cargar los vendedores: ${error.message ?: "error"}"
                                }
                        } else {
                            ventas = ventasCargadas
                                .filter { it.usuarioId == usuarioId }
                                .sortedByDescending { it.fechaMillis }
                            pagos = pagosCargados
                                .filter { it.vendedorId == usuarioId }
                                .sortedByDescending { it.fechaMillis }
                            cargando = false
                        }
                    }
                    .addOnFailureListener { error ->
                        cargando = false
                        mensaje = "No se pudieron cargar los pagos de comisiones: ${error.message ?: "error"}"
                    }
            }
            .addOnFailureListener { error ->
                cargando = false
                mensaje = "No se pudieron cargar las comisiones: ${error.message ?: "error"}"
            }
    }

    LaunchedEffect(usuarioId, esAdmin) {
        cargarComisiones()
    }

    val ventasValidas = ventas.filter {
        it.estado.lowercase() != "anulada" && it.comision > 0.0
    }

    val totalGenerado = ventasValidas.sumOf { it.comision }
    val totalPagado = pagos
        .filter { it.estado == "ACEPTADO" }
        .sumOf { it.monto }
    val totalPendiente = (totalGenerado - totalPagado).coerceAtLeast(0.0)

    val resumenVendedores = usuariosVendedores.map { vendedor ->
        val generada = ventas
            .filter { it.usuarioId == vendedor.id && it.estado.lowercase() != "anulada" }
            .sumOf { it.comision }
        val pagada = pagos
            .filter { it.vendedorId == vendedor.id && it.estado == "ACEPTADO" }
            .sumOf { it.monto }
        ComisionResumen(
            usuarioId = vendedor.id,
            email = vendedor.email,
            puntoVenta = vendedor.puntoVenta,
            generada = generada,
            pagada = pagada,
            pendiente = (generada - pagada).coerceAtLeast(0.0)
        )
    }

    val pagosPendientesAceptacion = pagos
        .filter { it.estado == "PENDIENTE_ACEPTACION" }
        .sortedByDescending { it.fechaMillis }

    val pagosAceptados = pagos
        .filter { it.estado == "ACEPTADO" }
        .sortedByDescending { it.fechaAceptacionMillis.takeIf { valor -> valor > 0L } ?: it.fechaMillis }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        MayLeVolverButton(onClick = onVolver)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "COMISIONES",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (esAdmin) {
                "Control de comisiones y pagos"
            } else {
                "Tus comisiones y pagos"
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (cargando) {
            Text("Cargando comisiones...")
        } else if (esAdmin) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TOTAL GENERADO", fontWeight = FontWeight.Bold, color = azulMayLe)
                    Text("$${formatoPrecio(totalGenerado)}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("TOTAL PAGADO: $${formatoPrecio(totalPagado)}")
                    Text("TOTAL PENDIENTE: $${formatoPrecio(totalPendiente)}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "COMISIONES POR VENDEDOR",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (resumenVendedores.isEmpty()) {
                Text("No hay vendedores activos.")
            } else {
                resumenVendedores.forEach { resumen ->
                    val vendedor = usuariosVendedores.firstOrNull { it.id == resumen.usuarioId }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(resumen.email, fontWeight = FontWeight.Bold)
                            Text("Punto de venta: ${resumen.puntoVenta.ifBlank { "SIN ASIGNAR" }}")
                            Text("Generadas: $${formatoPrecio(resumen.generada)}")
                            Text("Pagadas: $${formatoPrecio(resumen.pagada)}")
                            Text("Pendientes: $${formatoPrecio(resumen.pendiente)}")

                            if (resumen.pendiente > 0.0 && vendedor != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        vendedorSeleccionado = vendedor
                                        montoOrden = formatoPrecio(resumen.pendiente)
                                        mensaje = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !procesando
                                ) {
                                    Text("GENERAR ORDEN DE PAGO")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (vendedorSeleccionado != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("NUEVA ORDEN DE PAGO", fontWeight = FontWeight.Bold, color = azulMayLe)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Vendedor: ${vendedorSeleccionado?.email}")
                        Text("Pendiente disponible: $${formatoPrecio(resumenVendedores.firstOrNull { it.usuarioId == vendedorSeleccionado?.id }?.pendiente ?: 0.0)}")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = montoOrden,
                            onValueChange = { montoOrden = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                            label = { Text("Monto a pagar") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    vendedorSeleccionado = null
                                    montoOrden = ""
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !procesando
                            ) {
                                Text("CANCELAR")
                            }
                            Button(
                                onClick = {
                                    val vendedor = vendedorSeleccionado ?: return@Button
                                    val monto = montoOrden.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    val pendienteDisponible = resumenVendedores.firstOrNull { it.usuarioId == vendedor.id }?.pendiente ?: 0.0

                                    if (monto <= 0.0) {
                                        mensaje = "Ingrese un monto válido"
                                        return@Button
                                    }
                                    if (monto > pendienteDisponible + 0.001) {
                                        mensaje = "El monto supera la comisión pendiente"
                                        return@Button
                                    }

                                    procesando = true
                                    mensaje = ""

                                    val datos = hashMapOf<String, Any>(
                                        "vendedorId" to vendedor.id,
                                        "vendedorEmail" to vendedor.email,
                                        "monto" to monto,
                                        "estado" to "PENDIENTE_ACEPTACION",
                                        "ordenadoPorId" to usuarioId,
                                        "ordenadoPorEmail" to usuarioActualEmail,
                                        "fechaOrden" to FieldValue.serverTimestamp()
                                    )

                                    db.collection("pagosComisiones")
                                        .add(datos)
                                        .addOnSuccessListener {
                                            procesando = false
                                            vendedorSeleccionado = null
                                            montoOrden = ""
                                            mensaje = "Orden de pago creada. Queda pendiente de aceptación del vendedor."
                                            cargarComisiones()
                                        }
                                        .addOnFailureListener { error ->
                                            procesando = false
                                            mensaje = "No se pudo crear la orden: ${error.message ?: "error"}"
                                        }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !procesando
                            ) {
                                Text(if (procesando) "GUARDANDO..." else "CREAR ORDEN")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ORDENES PENDIENTES DE ACEPTACIÓN",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (pagosPendientesAceptacion.isEmpty()) {
                Text("No hay órdenes pendientes de aceptación.")
            } else {
                pagosPendientesAceptacion.forEach { pago ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Vendedor: ${pago.vendedorEmail}", fontWeight = FontWeight.Bold)
                            Text("Monto: $${formatoPrecio(pago.monto)}")
                            Text("Estado: PENDIENTE DE ACEPTACIÓN")
                            Text("Ordenada por: ${pago.ordenadoPorEmail}")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "HISTORIAL DE PAGOS DE COMISIONES",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (pagosAceptados.isEmpty()) {
                Text("No hay pagos de comisiones aceptados.")
            } else {
                pagosAceptados.forEach { pago ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Vendedor: ${pago.vendedorEmail}", fontWeight = FontWeight.Bold)
                            Text("Monto: $${formatoPrecio(pago.monto)}")
                            Text("Estado: ACEPTADO")
                            Text("Ordenada por: ${pago.ordenadoPorEmail}")
                            if (pago.fechaMillis > 0L) {
                                Text(
                                    "Fecha de orden: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(pago.fechaMillis))}"
                                )
                            }
                            Text("Aceptada por: ${pago.aceptadoPorEmail.ifBlank { pago.vendedorEmail }}")
                            if (pago.fechaAceptacionMillis > 0L) {
                                Text(
                                    "Fecha de aceptación: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(pago.fechaAceptacionMillis))}"
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "HISTORIAL DE VENTAS CON COMISIÓN",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (ventas.isEmpty()) {
                Text("No hay ventas registradas.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ventas) { venta ->
                        val anulada = venta.estado.lowercase() == "anulada"
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Fecha: ${venta.fechaTexto}", fontWeight = FontWeight.Bold)
                                Text("Vendedor: ${venta.usuarioEmail}")
                                Text("Venta: ${venta.id.takeLast(6)}")
                                Text("Total: $${formatoPrecio(venta.total)}")
                                Text(if (anulada) "ANULADA | Comisión: $0" else "Comisión: $${formatoPrecio(venta.comision)}")
                            }
                        }
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TOTAL GENERADO", fontWeight = FontWeight.Bold, color = azulMayLe)
                    Text("$${formatoPrecio(totalGenerado)}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("TOTAL PAGADO: $${formatoPrecio(totalPagado)}")
                    Text("COMISIÓN PENDIENTE: $${formatoPrecio(totalPendiente)}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PAGOS PENDIENTES DE ACEPTAR",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (pagosPendientesAceptacion.isEmpty()) {
                Text("No tenés pagos pendientes de aceptación.")
            } else {
                pagosPendientesAceptacion.forEach { pago ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("ORDEN DE PAGO", fontWeight = FontWeight.Bold, color = azulMayLe)
                            Text("Monto: $${formatoPrecio(pago.monto)}")
                            Text("Ordenada por: ${pago.ordenadoPorEmail}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    procesando = true
                                    mensaje = ""
                                    val ref = db.collection("pagosComisiones").document(pago.id)
                                    val cambios = hashMapOf<String, Any>(
                                        "estado" to "ACEPTADO",
                                        "aceptadoPorId" to usuarioId,
                                        "aceptadoPorEmail" to usuarioActualEmail,
                                        "fechaAceptacion" to FieldValue.serverTimestamp()
                                    )
                                    ref.update(cambios)
                                        .addOnSuccessListener {
                                            procesando = false
                                            mensaje = "Pago de comisión aceptado."
                                            cargarComisiones()
                                        }
                                        .addOnFailureListener { error ->
                                            procesando = false
                                            mensaje = "No se pudo aceptar el pago: ${error.message ?: "error"}"
                                        }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !procesando
                            ) {
                                Text("ACEPTAR PAGO")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "HISTORIAL DE COMISIONES",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = azulMayLe
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (ventas.isEmpty()) {
                Text("No hay ventas con comisión registrada.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ventas) { venta ->
                        val anulada = venta.estado.lowercase() == "anulada"
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Fecha: ${venta.fechaTexto}", fontWeight = FontWeight.Bold)
                                Text("Venta: ${venta.id.takeLast(6)}")
                                Text("Total venta: $${formatoPrecio(venta.total)}")
                                Text(if (anulada) "ANULADA | Comisión: $0" else "Comisión generada: $${formatoPrecio(venta.comision)}")
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
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun MayLeCaja(
    usuarioId: String,
    usuarioEmail: String,
    puntoVenta: String,
    esAdmin: Boolean,
    onVolver: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)
    val db = FirebaseFirestore.getInstance()
    var movimientos by remember { mutableStateOf(listOf<MovimientoCaja>()) }
    var rendiciones by remember { mutableStateOf(listOf<RendicionCaja>()) }
    var cargando by remember { mutableStateOf(true) }
    var mensaje by remember { mutableStateOf("") }
    var motivoEgreso by remember { mutableStateOf("") }
    var importeEgreso by remember { mutableStateOf("") }
    var importeRendicion by remember { mutableStateOf("") }
    var nuevaApertura by remember { mutableStateOf("") }
    var procesando by remember { mutableStateOf(false) }
    var mostrarEgreso by remember { mutableStateOf(false) }
    var mostrarRendicion by remember { mutableStateOf(false) }

    fun cargarCaja() {
        cargando = true

        val queryMovimientos = if (esAdmin) {
            db.collection("movimientosCaja")
                .orderBy("fechaHora", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
        } else {
            db.collection("movimientosCaja")
                .whereEqualTo("usuarioId", usuarioId)
                .limit(100)
        }

        queryMovimientos.get()
            .addOnSuccessListener { resultado ->
                movimientos = resultado.documents.map { doc ->
                    val ts = doc.getTimestamp("fechaHora")
                    MovimientoCaja(
                        id = doc.id,
                        usuarioId = doc.getString("usuarioId") ?: "",
                        usuarioEmail = doc.getString("usuarioEmail") ?: "",
                        puntoVenta = doc.getString("puntoVenta") ?: "",
                        tipo = doc.getString("tipo") ?: "",
                        importe = doc.getDouble("importe") ?: 0.0,
                        concepto = doc.getString("concepto") ?: "",
                        formaPago = doc.getString("formaPago") ?: "",
                        ventaId = doc.getString("ventaId") ?: "",
                        fechaMillis = ts?.toDate()?.time ?: 0L
                    )
                }.sortedByDescending { it.fechaMillis }

                if (esAdmin) {
                    db.collection("rendicionesCaja")
                        .orderBy("fechaHora", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(100)
                        .get()
                        .addOnSuccessListener { resultadoRendiciones ->
                            rendiciones = resultadoRendiciones.documents.map { doc ->
                                val ts = doc.getTimestamp("fechaHora")
                                RendicionCaja(
                                    id = doc.id,
                                    usuarioId = doc.getString("usuarioId") ?: "",
                                    usuarioEmail = doc.getString("usuarioEmail") ?: "",
                                    puntoVenta = doc.getString("puntoVenta") ?: "",
                                    saldoEsperado = doc.getDouble("saldoEsperado") ?: 0.0,
                                    montoDeclarado = doc.getDouble("montoDeclarado") ?: 0.0,
                                    diferencia = doc.getDouble("diferencia") ?: 0.0,
                                    nuevaApertura = doc.getDouble("nuevaApertura") ?: 0.0,
                                    estado = doc.getString("estado") ?: "pendiente",
                                    fechaMillis = ts?.toDate()?.time ?: 0L,
                                    confirmadoPorEmail = doc.getString("confirmadoPorEmail") ?: ""
                                )
                            }
                            cargando = false
                        }
                        .addOnFailureListener {
                            cargando = false
                            mensaje = "No se pudieron cargar las rendiciones"
                        }
                } else {
                    rendiciones = emptyList()
                    cargando = false
                }
            }
            .addOnFailureListener {
                cargando = false
                mensaje = "No se pudo cargar la caja"
            }
    }

    fun asegurarAperturaInicial() {
        if (usuarioId.isBlank()) {
            mensaje = "No se pudo identificar el usuario para abrir la caja"
            return
        }

        db.collection("movimientosCaja")
            .whereEqualTo("usuarioId", usuarioId)
            .limit(1)
            .get()
            .addOnSuccessListener { resultado ->

                if (!resultado.isEmpty) {
                    cargarCaja()
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .document(usuarioId)
                    .get()
                    .addOnSuccessListener { doc ->

                        val valorApertura = doc.get("montoAperturaCaja")
                        val apertura = when (valorApertura) {
                            is Number -> valorApertura.toDouble()
                            else -> valorApertura?.toString()?.toDoubleOrNull() ?: 0.0
                        }

                        val ref = db.collection("movimientosCaja").document()

                        val datos = hashMapOf<String, Any>(
                            "tipo" to "APERTURA",
                            "importe" to apertura,
                            "usuarioId" to usuarioId,
                            "usuarioEmail" to usuarioEmail,
                            "puntoVenta" to puntoVenta,
                            "concepto" to "Apertura inicial de caja",
                            "formaPago" to "EFECTIVO",
                            "fechaHora" to FieldValue.serverTimestamp()
                        )

                        ref.set(datos)
                            .addOnSuccessListener {
                                mensaje = "Caja abierta con $${formatoPrecio(apertura)}"
                                cargarCaja()
                            }
                            .addOnFailureListener { error ->
                                mensaje = "No se pudo crear la apertura: ${error.message ?: "error de permisos"}"
                            }
                    }
                    .addOnFailureListener { error ->
                        mensaje = "No se pudo leer la apertura del usuario: ${error.message ?: "error"}"
                    }
            }
            .addOnFailureListener { error ->
                mensaje = "No se pudo consultar la caja: ${error.message ?: "error"}"
            }
    }

    LaunchedEffect(usuarioId, esAdmin) {
        asegurarAperturaInicial()
        cargarCaja()
    }

    val saldoActual = movimientos
        .filter { it.usuarioId == usuarioId }
        .sumOf { it.importe }

    val pendientes = rendiciones.filter { it.estado == "pendiente" }

    if (cargando) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            MayLeVolverButton(onClick = onVolver)
            Spacer(modifier = Modifier.height(20.dp))
            Text("Cargando caja...", fontSize = 20.sp)
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Caja",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Usuario: ${if (usuarioEmail.isBlank()) "TODOS" else usuarioEmail}")
        Text("Punto de venta: $puntoVenta")

        Spacer(modifier = Modifier.height(18.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("SALDO ACTUAL", fontWeight = FontWeight.Bold, color = azulMayLe)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$${formatoPrecio(saldoActual)}",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "El saldo incluye apertura, ventas en efectivo, egresos y rendiciones confirmadas.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { mostrarEgreso = !mostrarEgreso },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (mostrarEgreso) "CANCELAR EGRESO" else "REGISTRAR EGRESO")
        }

        if (mostrarEgreso) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = importeEgreso,
                onValueChange = { importeEgreso = it.filter { c -> c.isDigit() } },
                label = { Text("Importe") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = motivoEgreso,
                onValueChange = { motivoEgreso = it },
                label = { Text("Motivo del egreso (obligatorio)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val importe = importeEgreso.toDoubleOrNull() ?: 0.0
                    if (importe <= 0.0) {
                        mensaje = "Ingrese un importe mayor a cero"
                        return@Button
                    }
                    if (motivoEgreso.isBlank()) {
                        mensaje = "Indique el motivo del egreso"
                        return@Button
                    }

                    procesando = true
                    val ref = db.collection("movimientosCaja").document()
                    val datos = hashMapOf(
                        "tipo" to "EGRESO",
                        "importe" to -importe,
                        "usuarioId" to usuarioId,
                        "usuarioEmail" to usuarioEmail,
                        "puntoVenta" to puntoVenta,
                        "concepto" to motivoEgreso.trim(),
                        "formaPago" to "EFECTIVO",
                        "fechaHora" to FieldValue.serverTimestamp()
                    )
                    ref.set(datos)
                        .addOnSuccessListener {
                            procesando = false
                            importeEgreso = ""
                            motivoEgreso = ""
                            mostrarEgreso = false
                            mensaje = "Egreso registrado"
                            cargarCaja()
                        }
                        .addOnFailureListener { error ->
                            procesando = false
                            mensaje = error.message ?: "No se pudo registrar el egreso"
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !procesando
            ) {
                Text(if (procesando) "GUARDANDO..." else "CONFIRMAR EGRESO")
            }
        }

        if (!esAdmin) {
            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { mostrarRendicion = !mostrarRendicion },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (mostrarRendicion) "CANCELAR RENDICIÓN" else "SOLICITAR RENDICIÓN")
            }

            if (mostrarRendicion) {
                Spacer(modifier = Modifier.height(12.dp))

                Text("Saldo esperado: $${formatoPrecio(saldoActual)}", fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = importeRendicion,
                    onValueChange = { importeRendicion = it.filter { c -> c.isDigit() } },
                    label = { Text("Monto entregado") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nuevaApertura,
                    onValueChange = { nuevaApertura = it.filter { c -> c.isDigit() } },
                    label = { Text("Nueva apertura de caja") },
                    supportingText = { Text("Monto de cambio para el nuevo ciclo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val declarado = importeRendicion.toDoubleOrNull()
                        val apertura = nuevaApertura.toDoubleOrNull()
                        if (declarado == null || declarado < 0.0) {
                            mensaje = "Ingrese el monto entregado"
                            return@Button
                        }
                        if (apertura == null || apertura < 0.0) {
                            mensaje = "Ingrese la nueva apertura"
                            return@Button
                        }

                        procesando = true
                        val ref = db.collection("rendicionesCaja").document()
                        val datos = hashMapOf(
                            "usuarioId" to usuarioId,
                            "usuarioEmail" to usuarioEmail,
                            "puntoVenta" to puntoVenta,
                            "saldoEsperado" to saldoActual,
                            "montoDeclarado" to declarado,
                            "diferencia" to (declarado - saldoActual),
                            "nuevaApertura" to apertura,
                            "estado" to "pendiente",
                            "fechaHora" to FieldValue.serverTimestamp()
                        )
                        ref.set(datos)
                            .addOnSuccessListener {
                                procesando = false
                                importeRendicion = ""
                                nuevaApertura = ""
                                mostrarRendicion = false
                                mensaje = "Rendición enviada al administrador"
                                cargarCaja()
                            }
                            .addOnFailureListener { error ->
                                procesando = false
                                mensaje = error.message ?: "No se pudo solicitar la rendición"
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !procesando
                ) {
                    Text(if (procesando) "ENVIANDO..." else "ENVIAR RENDICIÓN")
                }
            }
        }

        if (esAdmin) {
            Spacer(modifier = Modifier.height(22.dp))
            Text("RENDICIONES DE CAJA", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = azulMayLe)

            if (pendientes.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No hay rendiciones pendientes de confirmación.",
                    color = Color.Gray
                )
            }

            pendientes.forEach { rendicion ->
                Spacer(modifier = Modifier.height(10.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(rendicion.usuarioEmail, fontWeight = FontWeight.Bold)
                        Text("Punto de venta: ${rendicion.puntoVenta}")
                        Text("Saldo esperado: $${formatoPrecio(rendicion.saldoEsperado)}")
                        Text("Monto declarado: $${formatoPrecio(rendicion.montoDeclarado)}")
                        Text("Diferencia: $${formatoPrecio(rendicion.diferencia)}")
                        Text("Nueva apertura: $${formatoPrecio(rendicion.nuevaApertura)}")

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                procesando = true
                                db.runTransaction { transaction ->
                                    val refRendicion = db.collection("rendicionesCaja").document(rendicion.id)
                                    val actual = transaction.get(refRendicion)
                                    if ((actual.getString("estado") ?: "pendiente") != "pendiente") {
                                        throw IllegalStateException("La rendición ya fue procesada")
                                    }

                                    val refCierre = db.collection("movimientosCaja").document()
                                    val cierre = hashMapOf(
                                        "tipo" to "RENDICION",
                                        "importe" to -rendicion.saldoEsperado,
                                        "usuarioId" to rendicion.usuarioId,
                                        "usuarioEmail" to rendicion.usuarioEmail,
                                        "puntoVenta" to rendicion.puntoVenta,
                                        "concepto" to "Rendición de caja confirmada",
                                        "formaPago" to "EFECTIVO",
                                        "fechaHora" to FieldValue.serverTimestamp()
                                    )
                                    transaction.set(refCierre, cierre)

                                    val refApertura = db.collection("movimientosCaja").document()
                                    val apertura = hashMapOf(
                                        "tipo" to "APERTURA",
                                        "importe" to rendicion.nuevaApertura,
                                        "usuarioId" to rendicion.usuarioId,
                                        "usuarioEmail" to rendicion.usuarioEmail,
                                        "puntoVenta" to rendicion.puntoVenta,
                                        "concepto" to "Nueva apertura después de rendición",
                                        "formaPago" to "EFECTIVO",
                                        "fechaHora" to FieldValue.serverTimestamp()
                                    )
                                    transaction.set(refApertura, apertura)

                                    transaction.update(
                                        refRendicion,
                                        mapOf(
                                            "estado" to "confirmada",
                                            "confirmadoPorId" to usuarioId,
                                            "confirmadoPorEmail" to usuarioEmail,
                                            "fechaConfirmacion" to FieldValue.serverTimestamp()
                                        )
                                    )
                                    null
                                }
                                    .addOnSuccessListener {
                                        procesando = false
                                        mensaje = "Rendición confirmada"
                                        cargarCaja()
                                    }
                                    .addOnFailureListener { error ->
                                        procesando = false
                                        mensaje = error.message ?: "No se pudo confirmar la rendición"
                                    }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !procesando
                        ) {
                            Text("CONFIRMAR RENDICIÓN")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        Text("MOVIMIENTOS", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = azulMayLe)

        Spacer(modifier = Modifier.height(8.dp))

        if (movimientos.isEmpty()) {
            Text("Todavía no hay movimientos de caja.", color = Color.Gray)
        } else {
            movimientos.forEach { movimiento ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val fecha = if (movimiento.fechaMillis > 0L) {
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(java.util.Date(movimiento.fechaMillis))
                        } else "Fecha pendiente"
                        Text(fecha, fontSize = 13.sp, color = Color.Gray)
                        Text(movimiento.tipo, fontWeight = FontWeight.Bold)
                        if (esAdmin) Text(movimiento.usuarioEmail)
                        Text(movimiento.concepto)
                        Text(
                            text = "$${formatoPrecio(movimiento.importe)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(mensaje, color = azulMayLe, fontWeight = FontWeight.Bold)
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
    var puntosVentaConfigurados by remember { mutableStateOf(listOf<String>()) }
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

        db.collection("configuracion")
            .document("puntosVenta")
            .get()
            .addOnSuccessListener { documento ->
                puntosVentaConfigurados = listOf(
                    documento.getString("puntoVenta1") ?: "",
                    documento.getString("puntoVenta2") ?: "",
                    documento.getString("puntoVenta3") ?: ""
                ).map { it.trim().uppercase() }
                    .filter { it.isNotBlank() }
                    .distinct()
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

        MayLeSeleccionarPuntoVenta(
            puntoVentaActual = destino,
            opciones = puntosVentaConfigurados.filter {
                !it.equals(origen.trim(), ignoreCase = true)
            },
            onSeleccion = { destino = it }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (destino.isBlank()) "Destino: no seleccionado" else "Destino: $destino",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (destino.isBlank()) Color.Gray else azulMayLe
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
                        val seleccionada = varianteSeleccionada?.nombre
                            ?.equals(variante.nombre, ignoreCase = true) == true

                        if (seleccionada) {
                            Button(
                                onClick = {
                                    varianteSeleccionada = variante
                                    mensaje = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${variante.nombre} | Stock en $origen: ${variante.stock}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
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
    var mostrandoIngresoStock by remember { mutableStateOf(false) }
    var productoParaStock by remember { mutableStateOf<Producto?>(null) }
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

    if (mostrandoIngresoStock && productoParaStock != null) {
        MayLeIngresoStock(
            producto = productoParaStock!!,
            onVolver = {
                mostrandoIngresoStock = false
                productoParaStock = null
            },
            onStockGuardado = {
                mostrandoIngresoStock = false
                productoParaStock = null
                cargarProductos()
            }
        )
        return
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

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (esAdmin) {
            Text(
                text = "Para mercadería de un producto ya existente, use INGRESAR STOCK.",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
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

                            if (esAdmin) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        productoParaStock = producto
                                        mostrandoIngresoStock = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("INGRESAR STOCK")
                                }
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
fun MayLeIngresoStock(
    producto: Producto,
    onVolver: () -> Unit,
    onStockGuardado: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)
    val db = FirebaseFirestore.getInstance()

    var puntoVenta by remember { mutableStateOf("") }
    var cantidades by remember {
        mutableStateOf(
            producto.variantesDefault.associate { it.nombre to "" }
        )
    }
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    val variantes = producto.variantesDefault

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
            text = "Ingreso de stock",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = producto.nombre,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = "Código: ${producto.codigo}")

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = puntoVenta,
            onValueChange = { puntoVenta = it.take(10).uppercase() },
            label = { Text("Punto de venta (opcional)") },
            placeholder = { Text("Ej.: SANTIAGO") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (puntoVenta.isBlank()) {
                "Sin punto de venta: se agregará al stock general del producto."
            } else {
                "Se agregará al stock del punto de venta $puntoVenta."
            },
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Cantidad a ingresar",
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (variantes.isEmpty()) {
            Text("Este producto no tiene variantes registradas.")
        } else {
            variantes.forEach { variante ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = variante.nombre,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stock actual general: ${variante.stock}",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedTextField(
                        value = cantidades[variante.nombre] ?: "",
                        onValueChange = { valor ->
                            cantidades = cantidades.toMutableMap().apply {
                                put(
                                    variante.nombre,
                                    valor.filter { caracter -> caracter.isDigit() }
                                )
                            }
                        },
                        label = { Text("Agregar") },
                        modifier = Modifier.width(110.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                val cantidadesValidas = cantidades.mapNotNull { (nombreVariante, texto) ->
                    val cantidad = texto.toLongOrNull() ?: 0L
                    if (cantidad > 0) nombreVariante to cantidad else null
                }.toMap()

                if (cantidadesValidas.isEmpty()) {
                    mensaje = "Ingrese al menos una cantidad mayor a cero"
                    return@Button
                }

                guardando = true
                mensaje = ""

                db.runTransaction { transaction ->
                    val referencia = db.collection("products").document(producto.id)
                    val documento = transaction.get(referencia)

                    if (puntoVenta.isBlank()) {
                        val variantesActuales = mutableMapOf<String, Long>()
                        variantesDesdeDocumento(documento).forEach {
                            variantesActuales[it.nombre] = it.stock
                        }

                        cantidadesValidas.forEach { (nombreVariante, cantidad) ->
                            val actual = variantesActuales[nombreVariante] ?: 0L
                            variantesActuales[nombreVariante] = actual + cantidad
                        }

                        transaction.update(
                            referencia,
                            "variantes",
                            variantesActuales
                        )
                    } else {
                        val mapaPuntosVenta = mutableMapOf<String, MutableMap<String, Any>>()
                        val pvData = documento.get("puntosVenta") as? Map<*, *>

                        if (pvData != null) {
                            pvData.forEach { (pvKey, pvValue) ->
                                val nombrePv = pvKey?.toString()?.trim()?.uppercase()
                                    ?: return@forEach
                                if (pvValue is Map<*, *>) {
                                    val mapaVariantes = mutableMapOf<String, Any>()
                                    pvValue.forEach { (varKey, varValue) ->
                                        if (varKey != null) {
                                            val stock = when (varValue) {
                                                is Number -> varValue.toLong()
                                                else -> varValue?.toString()?.toLongOrNull() ?: 0L
                                            }
                                            mapaVariantes[varKey.toString()] = stock
                                        }
                                    }
                                    mapaPuntosVenta[nombrePv] = mapaVariantes
                                }
                            }
                        }

                        val mapaDestino = mapaPuntosVenta.getOrPut(puntoVenta.trim().uppercase()) {
                            val nuevoMapa = mutableMapOf<String, Any>()
                            variantesDesdeDocumento(documento).forEach {
                                nuevoMapa[it.nombre] = it.stock
                            }
                            nuevoMapa
                        }

                        cantidadesValidas.forEach { (nombreVariante, cantidad) ->
                            val actual = (mapaDestino[nombreVariante] as? Number)?.toLong() ?: 0L
                            mapaDestino[nombreVariante] = actual + cantidad
                        }

                        transaction.update(
                            referencia,
                            "puntosVenta",
                            mapaPuntosVenta
                        )
                    }

                    val refMovimiento = db.collection("movimientosStock").document()
                    val datosMovimiento = hashMapOf(
                        "tipo" to "INGRESO",
                        "usuarioId" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
                        "usuarioEmail" to (FirebaseAuth.getInstance().currentUser?.email ?: ""),
                        "puntoVenta" to puntoVenta.trim().uppercase(),
                        "productoId" to producto.id,
                        "codigo" to producto.codigo,
                        "nombre" to producto.nombre,
                        "cantidades" to cantidadesValidas,
                        "fechaHora" to FieldValue.serverTimestamp()
                    )
                    transaction.set(refMovimiento, datosMovimiento)
                    null
                }
                    .addOnSuccessListener {
                        guardando = false
                        onStockGuardado()
                    }
                    .addOnFailureListener {
                        guardando = false
                        mensaje = "No se pudo ingresar el stock"
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !guardando
        ) {
            Text(
                text = if (guardando) "GUARDANDO..." else "AGREGAR STOCK",
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
                    onValueChange = { variante.nombre = it.take(20) },
                    label = { Text("Variante") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = variante.stock,
                    onValueChange = {
                        variante.stock = it.filter { caracter -> caracter.isDigit() }
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


fun nombreFormaPagoReporte(formaPago: String): String {
    return when (formaPago) {
        "EFECTIVO" -> "EFECTIVO"
        "UN_PAGO" -> "1 PAGO"
        "TRES_CUOTAS" -> "3 CUOTAS"
        else -> formaPago.ifBlank { "SIN DATOS" }
    }
}

fun periodoReporte(
    opcion: String
): Pair<Long, Long> {
    val ahora = Calendar.getInstance()
    ahora.set(Calendar.MILLISECOND, 0)

    return when (opcion) {
        "HOY" -> {
            val inicio = ahora.clone() as Calendar
            inicio.set(Calendar.HOUR_OF_DAY, 0)
            inicio.set(Calendar.MINUTE, 0)
            inicio.set(Calendar.SECOND, 0)
            val fin = inicio.clone() as Calendar
            fin.add(Calendar.DAY_OF_MONTH, 1)
            fin.add(Calendar.MILLISECOND, -1)
            inicio.timeInMillis to fin.timeInMillis
        }

        "7_DIAS" -> {
            val fin = ahora.timeInMillis
            val inicio = ahora.clone() as Calendar
            inicio.add(Calendar.DAY_OF_MONTH, -6)
            inicio.set(Calendar.HOUR_OF_DAY, 0)
            inicio.set(Calendar.MINUTE, 0)
            inicio.set(Calendar.SECOND, 0)
            inicio.set(Calendar.MILLISECOND, 0)
            inicio.timeInMillis to fin
        }

        "30_DIAS" -> {
            val fin = ahora.timeInMillis
            val inicio = ahora.clone() as Calendar
            inicio.add(Calendar.DAY_OF_MONTH, -29)
            inicio.set(Calendar.HOUR_OF_DAY, 0)
            inicio.set(Calendar.MINUTE, 0)
            inicio.set(Calendar.SECOND, 0)
            inicio.set(Calendar.MILLISECOND, 0)
            inicio.timeInMillis to fin
        }

        else -> {
            val inicio = ahora.clone() as Calendar
            inicio.set(Calendar.DAY_OF_MONTH, 1)
            inicio.set(Calendar.HOUR_OF_DAY, 0)
            inicio.set(Calendar.MINUTE, 0)
            inicio.set(Calendar.SECOND, 0)
            inicio.set(Calendar.MILLISECOND, 0)
            val fin = inicio.clone() as Calendar
            fin.add(Calendar.MONTH, 1)
            fin.add(Calendar.MILLISECOND, -1)
            inicio.timeInMillis to fin.timeInMillis
        }
    }
}

fun parseFechaReporte(texto: String, finDelDia: Boolean): Long? {
    return try {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        formato.isLenient = false
        val fecha = formato.parse(texto.trim()) ?: return null
        val calendario = Calendar.getInstance()
        calendario.time = fecha
        if (finDelDia) {
            calendario.set(Calendar.HOUR_OF_DAY, 23)
            calendario.set(Calendar.MINUTE, 59)
            calendario.set(Calendar.SECOND, 59)
            calendario.set(Calendar.MILLISECOND, 999)
        } else {
            calendario.set(Calendar.HOUR_OF_DAY, 0)
            calendario.set(Calendar.MINUTE, 0)
            calendario.set(Calendar.SECOND, 0)
            calendario.set(Calendar.MILLISECOND, 0)
        }
        calendario.timeInMillis
    } catch (_: Exception) {
        null
    }
}

fun textoPeriodoReporte(inicio: Long, fin: Long): String {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return "${formato.format(Date(inicio))} al ${formato.format(Date(fin))}"
}

@Composable
fun MayLeReportes(
    onVolver: () -> Unit
) {
    BackHandler(enabled = true, onBack = onVolver)

    val azulMayLe = Color(0xFF123B5D)
    val db = FirebaseFirestore.getInstance()

    var seccion by remember { mutableStateOf("ventas") }
    var fechaDesdeTexto by remember { mutableStateOf("") }
    var fechaHastaTexto by remember { mutableStateOf("") }
    var periodoAplicado by remember { mutableStateOf(periodoReporte("ESTE_MES")) }
    var periodoSeleccionado by remember { mutableStateOf("ESTE_MES") }

    var ventas by remember { mutableStateOf(listOf<VentaHistorial>()) }
    var productos by remember { mutableStateOf(listOf<Producto>()) }
    var movimientosCaja by remember { mutableStateOf(listOf<MovimientoCaja>()) }
    var pagosComisiones by remember { mutableStateOf(listOf<PagoComision>()) }
    var cargando by remember { mutableStateOf(true) }
    var mensaje by remember { mutableStateOf("") }

    var vendedorFiltro by remember { mutableStateOf("TODOS") }
    var puntoVentaFiltro by remember { mutableStateOf("TODOS") }
    var formaPagoFiltro by remember { mutableStateOf("TODOS") }
    var mostrarDetalleVenta by remember { mutableStateOf<VentaHistorial?>(null) }
    var mostrarDialogoVendedor by remember { mutableStateOf(false) }
    var mostrarDialogoPuntoVenta by remember { mutableStateOf(false) }

    var filtroStock by remember { mutableStateOf("TODOS") }
    var ordenStock by remember { mutableStateOf("MENOR") }
    var busquedaStock by remember { mutableStateOf("") }
    var ordenProductos by remember { mutableStateOf("MAS_VENDIDOS") }
    var vistaStockRapida by remember { mutableStateOf(false) }

    fun cargarDatos() {
        cargando = true
        mensaje = ""

        var pendientes = 4
        fun terminado() {
            pendientes -= 1
            if (pendientes <= 0) cargando = false
        }

        db.collection("ventas")
            .get()
            .addOnSuccessListener { resultado ->
                ventas = resultado.documents.map { documento ->
                    val itemsRaw = documento.get("items") as? List<*> ?: emptyList<Any>()
                    val items = itemsRaw.mapNotNull { elemento ->
                        val mapa = elemento as? Map<*, *> ?: return@mapNotNull null
                        VentaHistorialItem(
                            productoId = mapa["productoId"]?.toString() ?: "",
                            codigo = mapa["codigo"]?.toString() ?: "",
                            nombre = mapa["nombre"]?.toString() ?: "",
                            variante = mapa["variante"]?.toString() ?: "",
                            cantidad = (mapa["cantidad"] as? Number)?.toLong()
                                ?: mapa["cantidad"]?.toString()?.toLongOrNull()
                                ?: 0L,
                            precioUnitario = (mapa["precioUnitario"] as? Number)?.toDouble()
                                ?: mapa["precioUnitario"]?.toString()?.toDoubleOrNull()
                                ?: 0.0,
                            subtotal = (mapa["subtotal"] as? Number)?.toDouble()
                                ?: mapa["subtotal"]?.toString()?.toDoubleOrNull()
                                ?: 0.0,
                            formaPago = mapa["formaPago"]?.toString() ?: ""
                        )
                    }
                    val timestamp = documento.getTimestamp("fechaHora")
                    VentaHistorial(
                        id = documento.id,
                        usuarioId = documento.getString("usuarioId") ?: "",
                        usuarioEmail = documento.getString("usuarioEmail") ?: "",
                        puntoVenta = documento.getString("puntoVenta") ?: "",
                        nombreComprador = documento.getString("nombreComprador") ?: "",
                        formaPago = documento.getString("formaPago") ?: "",
                        total = documento.getDouble("total") ?: 0.0,
                        comision = documento.getDouble("comision") ?: 0.0,
                        estado = documento.getString("estado") ?: "confirmada",
                        fechaTexto = timestamp?.toDate()?.let {
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                        } ?: "Fecha pendiente",
                        fechaMillis = timestamp?.toDate()?.time ?: 0L,
                        items = items,
                        motivoAnulacion = documento.getString("motivoAnulacion") ?: "",
                        anuladaPorEmail = documento.getString("anuladaPorEmail") ?: ""
                    )
                }.sortedByDescending { it.fechaMillis }
                terminado()
            }
            .addOnFailureListener {
                mensaje = "No se pudieron cargar las ventas"
                terminado()
            }

        db.collection("products")
            .get()
            .addOnSuccessListener { resultado ->
                productos = resultado.documents.map { productoDesdeDocumento(it) }
                terminado()
            }
            .addOnFailureListener {
                mensaje = "No se pudieron cargar los productos"
                terminado()
            }

        db.collection("movimientosCaja")
            .get()
            .addOnSuccessListener { resultado ->
                movimientosCaja = resultado.documents.map { documento ->
                    val timestamp = documento.getTimestamp("fechaHora")
                    MovimientoCaja(
                        id = documento.id,
                        usuarioId = documento.getString("usuarioId") ?: "",
                        usuarioEmail = documento.getString("usuarioEmail") ?: "",
                        puntoVenta = documento.getString("puntoVenta") ?: "",
                        tipo = documento.getString("tipo") ?: "",
                        importe = documento.getDouble("importe") ?: 0.0,
                        concepto = documento.getString("concepto") ?: "",
                        formaPago = documento.getString("formaPago") ?: "",
                        ventaId = documento.getString("ventaId") ?: "",
                        fechaMillis = timestamp?.toDate()?.time ?: 0L
                    )
                }.sortedByDescending { it.fechaMillis }
                terminado()
            }
            .addOnFailureListener {
                mensaje = "No se pudieron cargar los movimientos de caja"
                terminado()
            }

        db.collection("pagosComisiones")
            .get()
            .addOnSuccessListener { resultado ->
                pagosComisiones = resultado.documents.map { documento ->
                    val fecha = documento.getTimestamp("fechaOrden")?.toDate()?.time ?: 0L
                    val fechaAceptacion = documento.getTimestamp("fechaAceptacion")?.toDate()?.time ?: 0L
                    PagoComision(
                        id = documento.id,
                        vendedorId = documento.getString("vendedorId") ?: "",
                        vendedorEmail = documento.getString("vendedorEmail") ?: "",
                        monto = documento.getDouble("monto") ?: 0.0,
                        estado = documento.getString("estado") ?: "PENDIENTE_ACEPTACION",
                        ordenadoPorId = documento.getString("ordenadoPorId") ?: "",
                        ordenadoPorEmail = documento.getString("ordenadoPorEmail") ?: "",
                        fechaMillis = fecha,
                        aceptadoPorId = documento.getString("aceptadoPorId") ?: "",
                        aceptadoPorEmail = documento.getString("aceptadoPorEmail") ?: "",
                        fechaAceptacionMillis = fechaAceptacion
                    )
                }.sortedByDescending { it.fechaMillis }
                terminado()
            }
            .addOnFailureListener {
                mensaje = "No se pudieron cargar los pagos de comisiones"
                terminado()
            }
    }

    LaunchedEffect(Unit) {
        cargarDatos()
    }

    fun aplicarPeriodoPersonalizado() {
        val desde = parseFechaReporte(fechaDesdeTexto, false)
        val hasta = parseFechaReporte(fechaHastaTexto, true)
        if (desde == null || hasta == null) {
            mensaje = "Use el formato dd/MM/yyyy en las dos fechas"
            return
        }
        if (desde > hasta) {
            mensaje = "La fecha desde no puede ser posterior a la fecha hasta"
            return
        }
        mensaje = ""
        periodoAplicado = desde to hasta
        periodoSeleccionado = "PERSONALIZADO"
    }

    val inicioPeriodo = periodoAplicado.first
    val finPeriodo = periodoAplicado.second
    val ventasPeriodo = ventas.filter { it.fechaMillis in inicioPeriodo..finPeriodo }
    val ventasConfirmadas = ventasPeriodo.filter { it.estado.lowercase() == "confirmada" }
    val ventasAnuladas = ventasPeriodo.filter { it.estado.lowercase() == "anulada" }

    val vendedoresDisponibles = listOf("TODOS") + ventas.map { it.usuarioEmail }.filter { it.isNotBlank() }.distinct().sorted()
    val puntosDisponibles = listOf("TODOS") + ventas.map { it.puntoVenta }.filter { it.isNotBlank() }.distinct().sorted()

    val ventasFiltradas = ventasConfirmadas.filter { venta ->
        (vendedorFiltro == "TODOS" || venta.usuarioEmail == vendedorFiltro) &&
                (puntoVentaFiltro == "TODOS" || venta.puntoVenta == puntoVentaFiltro) &&
                (formaPagoFiltro == "TODOS" || venta.formaPago == formaPagoFiltro)
    }

    val stockFilas = productos.flatMap { producto ->
        if (producto.stockPorPuntoVenta.isNotEmpty()) {
            producto.stockPorPuntoVenta.flatMap { (pv, variantes) ->
                variantes.map { variante ->
                    ReporteStockFila(
                        productoId = producto.id,
                        codigo = producto.codigo,
                        nombre = producto.nombre,
                        variante = variante.nombre,
                        puntoVenta = pv,
                        stock = variante.stock,
                        precio = producto.precioUnPago
                    )
                }
            }
        } else {
            producto.variantesDefault.map { variante ->
                ReporteStockFila(
                    productoId = producto.id,
                    codigo = producto.codigo,
                    nombre = producto.nombre,
                    variante = variante.nombre,
                    puntoVenta = "SIN ASIGNAR",
                    stock = variante.stock,
                    precio = producto.precioUnPago
                )
            }
        }
    }

    val stockFiltrado = stockFilas.filter { fila ->
        val busqueda = busquedaStock.trim().lowercase()
        val pasaTexto = busqueda.isEmpty() ||
                fila.codigo.lowercase().contains(busqueda) ||
                fila.nombre.lowercase().contains(busqueda) ||
                fila.variante.lowercase().contains(busqueda) ||
                fila.puntoVenta.lowercase().contains(busqueda)
        val pasaCantidad = when (filtroStock) {
            "CERO" -> fila.stock <= 0
            "CRITICO" -> fila.stock in 1L..2L
            "MAS_2" -> fila.stock > 2L
            else -> true
        }
        pasaTexto && pasaCantidad
    }.sortedWith(
        when (ordenStock) {
            "MAYOR" -> compareByDescending<ReporteStockFila> { it.stock }.thenBy { it.nombre.lowercase() }
            "CARO" -> compareByDescending<ReporteStockFila> { it.precio }.thenBy { it.nombre.lowercase() }
            "BARATO" -> compareBy<ReporteStockFila> { it.precio }.thenBy { it.nombre.lowercase() }
            else -> compareBy<ReporteStockFila> { it.stock }.thenBy { it.nombre.lowercase() }
        }
    )

    val stockRapidoMap = linkedMapOf<String, ReporteStockRapidoFila>()
    stockFilas.forEach { fila ->
        val clave = "${fila.productoId}||${fila.variante.trim().lowercase()}"
        val actual = stockRapidoMap[clave]
        stockRapidoMap[clave] = ReporteStockRapidoFila(
            productoId = fila.productoId,
            codigo = fila.codigo,
            nombre = fila.nombre,
            variante = fila.variante,
            stockTotal = (actual?.stockTotal ?: 0L) + fila.stock
        )
    }

    val stockRapidoFiltrado = stockRapidoMap.values.filter { fila ->
        val busqueda = busquedaStock.trim().lowercase()
        val pasaTexto = busqueda.isEmpty() ||
                fila.codigo.lowercase().contains(busqueda) ||
                fila.nombre.lowercase().contains(busqueda) ||
                fila.variante.lowercase().contains(busqueda)
        val pasaCantidad = when (filtroStock) {
            "CERO" -> fila.stockTotal <= 0L
            "CRITICO" -> fila.stockTotal in 1L..2L
            "MAS_2" -> fila.stockTotal > 2L
            else -> true
        }
        pasaTexto && pasaCantidad
    }.sortedWith(
        when (ordenStock) {
            "MAYOR" -> compareByDescending<ReporteStockRapidoFila> { it.stockTotal }
                .thenBy { it.nombre.lowercase() }
                .thenBy { it.variante.lowercase() }
            else -> compareBy<ReporteStockRapidoFila> { it.stockTotal }
                .thenBy { it.nombre.lowercase() }
                .thenBy { it.variante.lowercase() }
        }
    )

    val ventasUnidadesPorProductoVariante = mutableMapOf<String, Long>()
    val ventasImportePorProductoVariante = mutableMapOf<String, Double>()
    ventasConfirmadas.forEach { venta ->
        venta.items.forEach { item ->
            val clave = "${item.productoId}||${item.variante.trim().lowercase()}"
            ventasUnidadesPorProductoVariante[clave] =
                (ventasUnidadesPorProductoVariante[clave] ?: 0L) + item.cantidad
            ventasImportePorProductoVariante[clave] =
                (ventasImportePorProductoVariante[clave] ?: 0.0) + item.subtotal
        }
    }

    val productosReporte = productos.flatMap { producto ->
        val variantes = if (producto.stockPorPuntoVenta.isNotEmpty()) {
            producto.stockPorPuntoVenta.values
                .flatten()
                .map { it.nombre }
                .distinct()
                .sortedBy { it.lowercase() }
        } else {
            producto.variantesDefault.map { it.nombre }.distinct().sortedBy { it.lowercase() }
        }

        variantes.map { nombreVariante ->
            val clave = "${producto.id}||${nombreVariante.trim().lowercase()}"
            val stockTotalVariante = if (producto.stockPorPuntoVenta.isNotEmpty()) {
                producto.stockPorPuntoVenta.values
                    .flatten()
                    .filter { it.nombre.equals(nombreVariante, ignoreCase = true) }
                    .sumOf { it.stock }
            } else {
                producto.variantesDefault
                    .filter { it.nombre.equals(nombreVariante, ignoreCase = true) }
                    .sumOf { it.stock }
            }

            ReporteProductoVarianteFila(
                producto = producto,
                variante = nombreVariante,
                unidadesVendidas = ventasUnidadesPorProductoVariante[clave] ?: 0L,
                importeVendido = ventasImportePorProductoVariante[clave] ?: 0.0,
                stockTotal = stockTotalVariante
            )
        }
    }.sortedWith(
        when (ordenProductos) {
            "MENOS_VENDIDOS" -> compareBy<ReporteProductoVarianteFila> { it.unidadesVendidas }
                .thenBy { it.producto.nombre.lowercase() }
                .thenBy { it.variante.lowercase() }
            "MAS_CAROS" -> compareByDescending<ReporteProductoVarianteFila> { it.producto.precioUnPago }
                .thenBy { it.producto.nombre.lowercase() }
                .thenBy { it.variante.lowercase() }
            "MAS_BARATOS" -> compareBy<ReporteProductoVarianteFila> { it.producto.precioUnPago }
                .thenBy { it.producto.nombre.lowercase() }
                .thenBy { it.variante.lowercase() }
            else -> compareByDescending<ReporteProductoVarianteFila> { it.unidadesVendidas }
                .thenBy { it.producto.nombre.lowercase() }
                .thenBy { it.variante.lowercase() }
        }
    )

    val ventasTotal = ventasConfirmadas.sumOf { it.total }
    val efectivoTotal = ventasConfirmadas.filter { it.formaPago == "EFECTIVO" }.sumOf { it.total }
    val unPagoTotal = ventasConfirmadas.filter { it.formaPago == "UN_PAGO" }.sumOf { it.total }
    val tresCuotasTotal = ventasConfirmadas.filter { it.formaPago == "TRES_CUOTAS" }.sumOf { it.total }
    val comisionesGeneradas = ventasConfirmadas.sumOf { it.comision }
    val egresos = movimientosCaja.filter {
        it.fechaMillis in inicioPeriodo..finPeriodo && it.tipo == "EGRESO"
    }.sumOf { -it.importe }
    val anulacionesEfectivo = movimientosCaja.filter {
        it.fechaMillis in inicioPeriodo..finPeriodo && it.tipo == "ANULACION_VENTA_EFECTIVO"
    }.sumOf { -it.importe }
    val ingresosManuales = movimientosCaja.filter {
        it.fechaMillis in inicioPeriodo..finPeriodo && it.tipo == "INGRESO"
    }.sumOf { it.importe }
    val movimientoNetoCaja = efectivoTotal - egresos - anulacionesEfectivo + ingresosManuales
    val comisionesPagadasPeriodo = pagosComisiones.filter {
        it.estado == "ACEPTADO" && it.fechaAceptacionMillis in inicioPeriodo..finPeriodo
    }.sumOf { it.monto }
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
            text = "REPORTES",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { cargarDatos() },
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (cargando) "ACTUALIZANDO..." else "ACTUALIZAR INFORMACIÓN")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("PERÍODO", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    periodoAplicado = periodoReporte("HOY")
                    periodoSeleccionado = "HOY"
                    mensaje = ""
                },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = if (periodoSeleccionado == "HOY") azulMayLe else Color.Transparent,
                    contentColor = if (periodoSeleccionado == "HOY") Color.White else azulMayLe
                )
            ) { Text("HOY") }
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(
                onClick = {
                    periodoAplicado = periodoReporte("7_DIAS")
                    periodoSeleccionado = "7_DIAS"
                    mensaje = ""
                },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = if (periodoSeleccionado == "7_DIAS") azulMayLe else Color.Transparent,
                    contentColor = if (periodoSeleccionado == "7_DIAS") Color.White else azulMayLe
                )
            ) { Text("7 DÍAS") }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    periodoAplicado = periodoReporte("30_DIAS")
                    periodoSeleccionado = "30_DIAS"
                    mensaje = ""
                },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = if (periodoSeleccionado == "30_DIAS") azulMayLe else Color.Transparent,
                    contentColor = if (periodoSeleccionado == "30_DIAS") Color.White else azulMayLe
                )
            ) { Text("30 DÍAS") }
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(
                onClick = {
                    periodoAplicado = periodoReporte("ESTE_MES")
                    periodoSeleccionado = "ESTE_MES"
                    mensaje = ""
                },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = if (periodoSeleccionado == "ESTE_MES") azulMayLe else Color.Transparent,
                    contentColor = if (periodoSeleccionado == "ESTE_MES") Color.White else azulMayLe
                )
            ) { Text("ESTE MES") }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = fechaDesdeTexto,
                onValueChange = { fechaDesdeTexto = it },
                label = { Text("Desde") },
                placeholder = { Text("dd/MM/yyyy") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = fechaHastaTexto,
                onValueChange = { fechaHastaTexto = it },
                label = { Text("Hasta") },
                placeholder = { Text("dd/MM/yyyy") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { aplicarPeriodoPersonalizado() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("APLICAR FECHAS")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Período aplicado: ${textoPeriodoReporte(inicioPeriodo, finPeriodo)}",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            val botones = listOf(
                "ventas" to "VENTAS",
                "stock" to "STOCK",
                "productos" to "PRODUCTOS",
                "finanzas" to "FINANZAS"
            )
            botones.forEachIndexed { indice, par ->
                OutlinedButton(
                    onClick = { seccion = par.first },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = if (seccion == par.first) azulMayLe else Color.Transparent,
                        contentColor = if (seccion == par.first) Color.White else azulMayLe
                    )
                ) {
                    Text(
                        text = par.second,
                        fontSize = 11.sp,
                        fontWeight = if (seccion == par.first) FontWeight.Bold else FontWeight.Normal
                    )
                }
                if (indice < botones.lastIndex) Spacer(modifier = Modifier.width(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (cargando) {
            Text("Cargando información...")
        } else {
            when (seccion) {
                "ventas" -> {
                    Text("REPORTE DE VENTAS", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = azulMayLe)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ventas confirmadas: ${ventasFiltradas.size}")
                    Text("Total vendido: $${formatoPrecio(ventasFiltradas.sumOf { it.total })}")
                    Text("Ticket promedio: $${formatoPrecio(if (ventasFiltradas.isEmpty()) 0.0 else ventasFiltradas.sumOf { it.total } / ventasFiltradas.size)}")
                    Text("Comisión generada: $${formatoPrecio(ventasFiltradas.sumOf { it.comision })}")

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("FILTROS", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { mostrarDialogoVendedor = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("VENDEDOR: $vendedorFiltro")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { mostrarDialogoPuntoVenta = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("PUNTO DE VENTA: $puntoVentaFiltro")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Forma de pago: ${nombreFormaPagoReporte(formaPagoFiltro)}", fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("TODOS", "EFECTIVO", "UN_PAGO", "TRES_CUOTAS").forEach { forma ->
                            OutlinedButton(
                                onClick = { formaPagoFiltro = forma },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (formaPagoFiltro == forma) azulMayLe else Color.Transparent,
                                    contentColor = if (formaPagoFiltro == forma) Color.White else azulMayLe
                                )
                            ) { Text(nombreFormaPagoReporte(forma), fontSize = 9.sp) }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    ventasFiltradas.forEach { venta ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${venta.fechaTexto}  •  ${venta.usuarioEmail}", fontWeight = FontWeight.Bold)
                                Text("POS: ${venta.puntoVenta}  •  ${nombreFormaPagoReporte(venta.formaPago)}")
                                Text("Cliente: ${venta.nombreComprador.ifBlank { "Sin nombre" }}")
                                Text("Artículos: ${venta.items.sumOf { it.cantidad }}")
                                Text("TOTAL: $${formatoPrecio(venta.total)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = azulMayLe)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(onClick = { mostrarDetalleVenta = venta }) {
                                    Text("VER DETALLE")
                                }
                            }
                        }
                    }
                    if (ventasFiltradas.isEmpty()) {
                        Text("No hay ventas que coincidan con los filtros.", color = Color.Gray)
                    }
                    if (ventasAnuladas.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ventas anuladas en el período: ${ventasAnuladas.size}  •  $${formatoPrecio(ventasAnuladas.sumOf { it.total })}", color = Color.Red)
                    }
                }

                "stock" -> {
                    Text(
                        text = if (vistaStockRapida) "CONSULTA RÁPIDA DE STOCK" else "REPORTE DE STOCK",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulMayLe
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (vistaStockRapida) {
                            "Filas: ${stockRapidoFiltrado.size}"
                        } else {
                            "Filas de stock: ${stockFiltrado.size}"
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { vistaStockRapida = !vistaStockRapida },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = if (vistaStockRapida) azulMayLe else Color.Transparent,
                            contentColor = if (vistaStockRapida) Color.White else azulMayLe
                        )
                    ) {
                        Text(
                            text = if (vistaStockRapida) "VISTA DETALLADA" else "VISTA RÁPIDA DE STOCK",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = busquedaStock,
                        onValueChange = { busquedaStock = it },
                        label = {
                            Text(
                                if (vistaStockRapida) {
                                    "Buscar producto, código o variante"
                                } else {
                                    "Buscar producto, código, variante o POS"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cantidad", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("TODOS", "CERO", "CRITICO", "MAS_2").forEach { filtro ->
                            OutlinedButton(
                                onClick = { filtroStock = filtro },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (filtroStock == filtro) azulMayLe else Color.Transparent,
                                    contentColor = if (filtroStock == filtro) Color.White else azulMayLe
                                )
                            ) {
                                Text(
                                    text = when (filtro) {
                                        "CERO" -> "≤ 0"
                                        "CRITICO" -> "1–2"
                                        "MAS_2" -> "> 2"
                                        else -> "TODOS"
                                    },
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Orden", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("MENOR", "MAYOR", "CARO", "BARATO").forEach { orden ->
                            OutlinedButton(
                                onClick = { ordenStock = orden },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (ordenStock == orden) azulMayLe else Color.Transparent,
                                    contentColor = if (ordenStock == orden) Color.White else azulMayLe
                                )
                            ) {
                                Text(when (orden) {
                                    "MENOR" -> "MENOS STOCK"
                                    "MAYOR" -> "MÁS STOCK"
                                    "CARO" -> "MÁS CARO"
                                    else -> "MÁS BARATO"
                                }, fontSize = 9.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (vistaStockRapida) {
                        stockRapidoFiltrado.forEach { fila ->
                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fila.nombre,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = fila.variante,
                                            color = azulMayLe,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = fila.stockTotal.toString(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (fila.stockTotal <= 0) Color.Red else azulMayLe
                                    )
                                }
                            }
                        }
                    } else {
                        stockFiltrado.forEach { fila ->
                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${fila.nombre} (${fila.codigo})", fontWeight = FontWeight.Bold)
                                    Text("Variante: ${fila.variante}  •  POS: ${fila.puntoVenta}")
                                    Text("STOCK: ${fila.stock}", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = if (fila.stock <= 0) Color.Red else azulMayLe)
                                    Text("Precio lista: $${formatoPrecio(fila.precio)}")
                                }
                            }
                        }
                    }
                }

                "productos" -> {
                    Text("REPORTE DE PRODUCTOS", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = azulMayLe)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Variantes analizadas: ${productosReporte.size}")
                    Text("Ventas usadas: ${ventasConfirmadas.size}")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("MAS_VENDIDOS", "MENOS_VENDIDOS", "MAS_CAROS", "MAS_BARATOS").forEach { orden ->
                            OutlinedButton(
                                onClick = { ordenProductos = orden },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (ordenProductos == orden) azulMayLe else Color.Transparent,
                                    contentColor = if (ordenProductos == orden) Color.White else Color(0xFF123B5D)
                                )
                            ) {
                                Text(
                                    when (orden) {
                                        "MAS_VENDIDOS" -> "MÁS\nVENDIDOS"
                                        "MENOS_VENDIDOS" -> "MENOS\nVENDIDOS"
                                        "MAS_CAROS" -> "MÁS\nCAROS"
                                        else -> "MÁS\nBARATOS"
                                    },
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    productosReporte.forEach { fila ->
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${fila.producto.nombre} (${fila.producto.codigo})", fontWeight = FontWeight.Bold)
                                Text("Variante: ${fila.variante}", fontWeight = FontWeight.Bold)
                                Text("Vendidas en el período: ${fila.unidadesVendidas}")
                                Text("Importe vendido: $${formatoPrecio(fila.importeVendido)}")
                                Text("Stock actual de la variante: ${fila.stockTotal}")
                                Text("Precio lista: $${formatoPrecio(fila.producto.precioUnPago)}")
                            }
                        }
                    }
                }

                "finanzas" -> {
                    Text("REPORTE FINANCIERO", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = azulMayLe)
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("VENTAS CONFIRMADAS", fontWeight = FontWeight.Bold)
                            Text("$${formatoPrecio(ventasTotal)}", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = azulMayLe)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Efectivo: $${formatoPrecio(efectivoTotal)}")
                            Text("1 pago: $${formatoPrecio(unPagoTotal)}")
                            Text("3 cuotas: $${formatoPrecio(tresCuotasTotal)}")
                            Text("Comisiones generadas: $${formatoPrecio(comisionesGeneradas)}")
                            Text("Comisiones pagadas en el período: $${formatoPrecio(comisionesPagadasPeriodo)}")
                            Text("Egresos de caja: $${formatoPrecio(egresos)}")
                            Text("Anulaciones en efectivo: $${formatoPrecio(anulacionesEfectivo)}")
                            Text("Ingresos manuales: $${formatoPrecio(ingresosManuales)}")
                            Text("Movimiento neto de caja: $${formatoPrecio(movimientoNetoCaja)}")
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Ventas anuladas: ${ventasAnuladas.size}  •  $${formatoPrecio(ventasAnuladas.sumOf { it.total })}", color = Color.Red)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("SITUACIÓN ACTUAL DE COMISIONES", fontWeight = FontWeight.Bold)
                            val pendientes = (pagosComisiones.filter { it.estado == "PENDIENTE_ACEPTACION" }.sumOf { it.monto }).coerceAtLeast(0.0)
                            Text("Órdenes pendientes de aceptación: $${formatoPrecio(pendientes)}")
                            Text("Pagos aceptados acumulados: $${formatoPrecio(pagosComisiones.filter { it.estado == "ACEPTADO" }.sumOf { it.monto })}")
                            Text("Nota: este bloque muestra el estado actual; no es una ganancia contable.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = mensaje,
                color = if (mensaje.startsWith("No hay")) Color.Gray else Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (mostrarDialogoVendedor) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoVendedor = false },
            title = { Text("Seleccionar vendedor") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    vendedoresDisponibles.forEach { vendedor ->
                        TextButton(
                            onClick = {
                                vendedorFiltro = vendedor
                                mostrarDialogoVendedor = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(vendedor) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoVendedor = false }) { Text("CERRAR") }
            }
        )
    }

    if (mostrarDialogoPuntoVenta) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPuntoVenta = false },
            title = { Text("Seleccionar punto de venta") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    puntosDisponibles.forEach { punto ->
                        TextButton(
                            onClick = {
                                puntoVentaFiltro = punto
                                mostrarDialogoPuntoVenta = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(punto) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoPuntoVenta = false }) { Text("CERRAR") }
            }
        )
    }

    if (mostrarDetalleVenta != null) {
        val venta = mostrarDetalleVenta!!
        AlertDialog(
            onDismissRequest = { mostrarDetalleVenta = null },
            title = { Text("Detalle de venta") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Fecha: ${venta.fechaTexto}")
                    Text("Vendedor: ${venta.usuarioEmail}")
                    Text("POS: ${venta.puntoVenta}")
                    Text("Cliente: ${venta.nombreComprador.ifBlank { "Sin nombre" }}")
                    Text("Pago: ${nombreFormaPagoReporte(venta.formaPago)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    venta.items.forEach { item ->
                        Text("${item.codigo} • ${item.nombre}")
                        Text("${item.variante} x ${item.cantidad} = $${formatoPrecio(item.subtotal)}")
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text("TOTAL: $${formatoPrecio(venta.total)}", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarDetalleVenta = null }) { Text("CERRAR") }
            }
        )
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

